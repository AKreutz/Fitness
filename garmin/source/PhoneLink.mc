import Toybox.Communications;
import Toybox.Lang;
import Toybox.System;
import Toybox.Time;
import Toybox.WatchUi;

// Sends commands to the phone and notices when one could not be delivered. The messages are documented on
// WatchProtocol in the Android app (data/watch/WatchProtocol.kt).
class PhoneLink extends Communications.ConnectionListener {
    private const SEND_TIMEOUT_MILLIS = 5000;

    private var mModel as SessionModel;
    private var mCounter as Number = 0;
    private var mIdBase as Long;
    // Connect IQ sends one message at a time: a second one started before the first is done fails. So messages wait
    // here until the one before has been delivered or has failed.
    private var mQueue as Array<Dictionary> = [];
    private var mSending as Boolean = false;
    private var mInFlight as Dictionary? = null;
    private var mSentAt as Number = 0;
    private var mHeartRateSender as HeartRateSender? = null;

    function initialize(model as SessionModel) {
        ConnectionListener.initialize();
        mModel = model;
        // The phone drops a message id it has already seen, so ids must not repeat across app runs.
        mIdBase = Time.now().value().toLong() * 1000l;
    }

    // The sender of heart rate readings is told whether each of its pieces got through.
    function setHeartRateSender(sender as HeartRateSender) as Void {
        mHeartRateSender = sender;
    }

    function sync() as Void {
        transmit({ "t" => "sync" });
    }

    // Finishes the current set, or starts the next one while resting.
    function advance() as Void {
        transmit({ "t" => "adv", "m" => nextId(), "v" => mModel.version });
    }

    function skip() as Void {
        transmit({ "t" => "skip", "m" => nextId(), "v" => mModel.version });
    }

    // effort is 0 for easy, 1 for medium and 2 for hard.
    function rate(effort as Number) as Void {
        transmit({ "t" => "rate", "e" => effort, "m" => nextId(), "v" => mModel.version });
    }

    function answerWeightOffer(accept as Boolean) as Void {
        transmit({ "t" => "inc", "y" => accept ? 1 : 0, "m" => nextId(), "v" => mModel.version });
    }

    // A piece of the heart rate readings; the sender is told how it went.
    function sendHeartRatePiece(piece as Dictionary) as Void {
        transmit(piece);
    }

    function onComplete() as Void {
        mModel.phoneReachable = true;
        var finished = mInFlight;
        mSending = false;
        mInFlight = null;
        if (isHeartRatePiece(finished) && mHeartRateSender != null) {
            mHeartRateSender.onDelivered();
        }
        sendNext();
    }

    function onError() as Void {
        mModel.phoneReachable = false;
        var failed = mInFlight;
        mSending = false;
        mInFlight = null;
        WatchUi.requestUpdate();
        if (isHeartRatePiece(failed) && mHeartRateSender != null) {
            mHeartRateSender.onFailed();
        }
        sendNext();
    }

    private function isHeartRatePiece(message as Dictionary?) as Boolean {
        return message != null && (message["t"] as String).equals("hl");
    }

    private function nextId() as Long {
        mCounter++;
        return mIdBase + mCounter;
    }

    private function transmit(message as Dictionary) as Void {
        mQueue.add(message);
        sendNext();
    }

    private function sendNext() as Void {
        // A message that never reports back does not block the rest forever.
        if (mSending && System.getTimer() - mSentAt < SEND_TIMEOUT_MILLIS) {
            return;
        }
        if (mQueue.size() == 0) {
            mSending = false;
            return;
        }
        var message = mQueue[0];
        mQueue = mQueue.slice(1, null);
        mSending = true;
        mInFlight = message;
        mSentAt = System.getTimer();
        Communications.transmit(message as Communications.TransmitType, null, self);
    }
}
