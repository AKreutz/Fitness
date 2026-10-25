import Toybox.Lang;
import Toybox.System;

// Sends the heart rate readings of a finished workout to the phone, one piece at a time: the next piece goes once the
// one before has got through. The message is documented on WatchProtocol in the Android app
// (data/watch/WatchProtocol.kt); the readings are one per second, three to a number.
class HeartRateSender {
    private const READINGS_PER_NUMBER = 3;
    private const NUMBERS_PER_PIECE = 64;
    // How often a piece is tried before the transfer waits, and how long it waits before starting over; and how often
    // it starts over before giving up.
    private const MAX_ATTEMPTS = 3;
    private const RETRY_MILLIS = 5000;
    private const RESTART_MILLIS = 30000;
    private const MAX_RESTARTS = 2;

    private var mModel as SessionModel;
    private var mLink as PhoneLink;
    private var mSessionId as String = "";
    private var mSamples as ByteArray = []b;
    private var mStartEpochSeconds as Long = 0l;
    private var mPieceCount as Number = 0;
    private var mNext as Number = 0;
    private var mAttempts as Number = 0;
    private var mRestarts as Number = 0;
    private var mWaitUntil as Number = 0;
    private var mActive as Boolean = false;
    private var mInFlight as Boolean = false;

    function initialize(model as SessionModel, link as PhoneLink) {
        mModel = model;
        mLink = link;
    }

    // Starts sending the readings of the workout the phone saved as sessionId. Does nothing for one already being, or
    // having been, sent.
    function begin(sessionId as String, tracker as HeartRateTracker) as Void {
        if (mSessionId.equals(sessionId)) {
            return;
        }
        mSessionId = sessionId;
        mSamples = tracker.samples;
        mStartEpochSeconds = tracker.startEpochSeconds;
        var perPiece = READINGS_PER_NUMBER * NUMBERS_PER_PIECE;
        mPieceCount = (mSamples.size() + perPiece - 1) / perPiece;
        mNext = 0;
        mAttempts = 0;
        mRestarts = 0;
        mWaitUntil = 0;
        mActive = true;
        mInFlight = false;
        mModel.heartRateStatus = HEART_RATE_SENDING;
        sendNext();
    }

    // Gives up on whatever is being sent, when the phone has signed off.
    function cancel() as Void {
        mSessionId = "";
        mSamples = []b;
        mActive = false;
        mInFlight = false;
    }

    // The piece that was sent got through.
    function onDelivered() as Void {
        mInFlight = false;
        mAttempts = 0;
        if (mNext + 1 >= mPieceCount) {
            mActive = false;
            mModel.heartRateStatus = HEART_RATE_DELIVERED;
            return;
        }
        mNext++;
        sendNext();
    }

    // The piece that was sent did not get through. It is sent again a little later, a few times, and then the whole
    // transfer starts over once or twice more before it gives up.
    function onFailed() as Void {
        mInFlight = false;
        mAttempts++;
        if (mAttempts < MAX_ATTEMPTS) {
            mWaitUntil = System.getTimer() + RETRY_MILLIS;
        } else if (mRestarts < MAX_RESTARTS) {
            mRestarts++;
            mAttempts = 0;
            mNext = 0;
            mWaitUntil = System.getTimer() + RESTART_MILLIS;
        } else {
            mActive = false;
            mModel.heartRateStatus = HEART_RATE_FAILED;
        }
    }

    // Called every second, to send again once the wait after a failure is over.
    function tick() as Void {
        if (mActive && !mInFlight && mWaitUntil > 0 && System.getTimer() >= mWaitUntil) {
            mWaitUntil = 0;
            sendNext();
        }
    }

    private function sendNext() as Void {
        if (!mActive || mInFlight) {
            return;
        }
        mInFlight = true;
        if (mPieceCount == 0) {
            // No readings: just say so, so the phone knows there is nothing to wait for.
            mLink.sendHeartRatePiece({ "t" => "hl", "sid" => mSessionId, "k" => 0 });
            return;
        }
        mLink.sendHeartRatePiece(piece(mNext));
    }

    private function piece(index as Number) as Dictionary {
        var from = index * READINGS_PER_NUMBER * NUMBERS_PER_PIECE;
        var to = from + READINGS_PER_NUMBER * NUMBERS_PER_PIECE;
        if (to > mSamples.size()) {
            to = mSamples.size();
        }
        var numbers = [] as Array<Number>;
        for (var i = from; i < to; i += READINGS_PER_NUMBER) {
            var value = mSamples[i];
            if (i + 1 < to) {
                value |= mSamples[i + 1] << 8;
            }
            if (i + 2 < to) {
                value |= mSamples[i + 2] << 16;
            }
            numbers.add(value);
        }
        return {
            "t" => "hl",
            "sid" => mSessionId,
            "t0" => mStartEpochSeconds,
            "k" => mPieceCount,
            "i" => index,
            "n" => mSamples.size(),
            "d" => numbers
        };
    }
}
