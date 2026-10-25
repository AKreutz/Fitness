import Toybox.Lang;
import Toybox.System;
import Toybox.Time;

// Where the session is, as the phone describes it. The values match WatchProtocol.PHASE_* in the Android app.
enum {
    PHASE_WARM_UP = 0,
    PHASE_WORKING = 1,
    PHASE_RESTING = 2,
    PHASE_RATE = 3,
    PHASE_WEIGHT_OFFER = 4,
    PHASE_FINISHED = 5,
    PHASE_UNAVAILABLE = 6
}

// How the heart rate readings of a finished workout are getting to the phone.
enum {
    HEART_RATE_NOT_SENT = 0,
    HEART_RATE_SENDING = 1,
    HEART_RATE_DELIVERED = 2,
    HEART_RATE_FAILED = 3,
    HEART_RATE_SAVED = 4
}

// The phone owns the session; this only mirrors the latest state message, whose keys are documented on WatchProtocol in
// the Android app (data/watch/WatchProtocol.kt).
class SessionModel {
    // A second tap within this long, before the phone has answered the first, is ignored.
    private const COMMAND_PAUSE_MILLIS = 1500;

    public var hasState as Boolean = false;
    // The version of the state shown, which goes up with every change on the phone. Commands carry it, so the phone can
    // tell a press on a screen it has already left. -1 until the first state.
    public var version as Number = -1;
    public var phoneReachable as Boolean = true;
    public var phase as Number = PHASE_WARM_UP;
    public var workoutName as String = "";
    public var exerciseName as String = "";
    public var exerciseId as String = "";
    public var exerciseNumber as Number = 0;
    public var exerciseCount as Number = 0;
    public var setNumber as Number = 0;
    public var setCount as Number = 0;
    public var weightKg as Float = 0.0;
    public var targetReps as Number = 0;
    public var restGoalSeconds as Number = 0;
    public var restStartedAt as Long = 0l;
    public var offeredWeightKg as Float = 0.0;
    public var personalRecords as Number = 0;
    // The id the finished workout is saved under on the phone, which its heart rate readings are sent for.
    public var sessionId as String = "";
    // Whether the phone has saved the finished workout. It waits for the heart rate readings before it does.
    public var workoutSaved as Boolean = false;
    // Where the heart rate readings of the finished workout are, on their way to the phone.
    public var heartRateStatus as Number = HEART_RATE_NOT_SENT;
    // The highlighted answer while rating (0 easy, 1 medium, 2 hard) or answering the weight offer (0 yes, 1 no).
    public var selection as Number = 0;
    private var mSentAt as Number = 0;
    private var mAwaitingAnswer as Boolean = false;

    // Returns true when the phase or exercise changed, so the caller can confirm it with a buzz.
    function update(data as Dictionary) as Boolean {
        phoneReachable = true;
        mAwaitingAnswer = false;
        var type = data["t"] as String?;
        if (type != null && type.equals("idle")) {
            reset();
            return false;
        }
        if (type == null || !type.equals("state")) {
            return false;
        }
        var incoming = numberOf(data["v"] as Object?, -1);
        if (hasState && incoming >= 0 && incoming < version) {
            // An older state that arrived after a newer one.
            return false;
        }
        version = incoming;
        var oldPhase = phase;
        var oldExerciseId = exerciseId;
        hasState = true;
        phase = numberOf(data["p"] as Object?, PHASE_UNAVAILABLE);
        workoutName = stringOf(data["w"] as Object?);
        exerciseName = stringOf(data["e"] as Object?);
        exerciseId = stringOf(data["id"] as Object?);
        exerciseNumber = numberOf(data["n"] as Object?, 0);
        exerciseCount = numberOf(data["N"] as Object?, 0);
        setNumber = numberOf(data["s"] as Object?, 0);
        setCount = numberOf(data["S"] as Object?, 0);
        weightKg = floatOf(data["kg"] as Object?);
        targetReps = numberOf(data["r"] as Object?, 0);
        restGoalSeconds = numberOf(data["rg"] as Object?, 0);
        restStartedAt = longOf(data["rt"] as Object?);
        offeredWeightKg = floatOf(data["nw"] as Object?);
        personalRecords = numberOf(data["pr"] as Object?, 0);
        sessionId = stringOf(data["sid"] as Object?);
        workoutSaved = numberOf(data["ok"] as Object?, 0) == 1;
        if (workoutSaved && heartRateStatus != HEART_RATE_NOT_SENT) {
            // The phone has saved the workout. Whether the readings were saved with it is what it says now, whatever the
            // watch made of the sending.
            heartRateStatus = numberOf(data["hr"] as Object?, 0) == 1 ? HEART_RATE_SAVED : HEART_RATE_FAILED;
        }

        var changed = phase != oldPhase || !exerciseId.equals(oldExerciseId);
        if (changed) {
            // Medium is the most likely rating, and raising the weight the most likely answer to the offer.
            selection = phase == PHASE_RATE ? 1 : 0;
        }
        return changed;
    }

    // The phone has signed off: forget the workout and go back to waiting for the next one.
    function reset() as Void {
        hasState = false;
        version = -1;
        phase = PHASE_WARM_UP;
        workoutName = "";
        exerciseName = "";
        exerciseId = "";
        exerciseNumber = 0;
        exerciseCount = 0;
        setNumber = 0;
        setCount = 0;
        weightKg = 0.0;
        targetReps = 0;
        restGoalSeconds = 0;
        restStartedAt = 0l;
        offeredWeightKg = 0.0;
        personalRecords = 0;
        sessionId = "";
        workoutSaved = false;
        heartRateStatus = HEART_RATE_NOT_SENT;
        selection = 0;
    }

    function isPrompt() as Boolean {
        return phase == PHASE_RATE || phase == PHASE_WEIGHT_OFFER;
    }

    // Whether the phone is running a workout the watch can follow, as opposed to finished or unavailable.
    function isActive() as Boolean {
        return hasState && phase >= PHASE_WARM_UP && phase <= PHASE_WEIGHT_OFFER;
    }

    function restElapsedSeconds() as Number {
        var elapsed = Time.now().value().toLong() - restStartedAt;
        return elapsed < 0 ? 0 : elapsed.toNumber();
    }

    function moveSelection(step as Number) as Void {
        var count = phase == PHASE_RATE ? 3 : 2;
        selection = (selection + step + count) % count;
    }

    // True when a command may go out now; also starts the pause that keeps a double tap from counting twice.
    function takeCommandTurn() as Boolean {
        var now = System.getTimer();
        if (mAwaitingAnswer && now - mSentAt < COMMAND_PAUSE_MILLIS) {
            return false;
        }
        mAwaitingAnswer = true;
        mSentAt = now;
        return true;
    }

    private function numberOf(value as Object?, fallback as Number) as Number {
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof Long || value instanceof Float || value instanceof Double) {
            return value.toNumber();
        }
        return fallback;
    }

    private function longOf(value as Object?) as Long {
        if (value instanceof Number || value instanceof Long) {
            return value.toLong();
        }
        return 0l;
    }

    private function floatOf(value as Object?) as Float {
        if (value instanceof Number || value instanceof Long || value instanceof Float || value instanceof Double) {
            return value.toFloat();
        }
        return 0.0;
    }

    private function stringOf(value as Object?) as String {
        return value instanceof String ? value : "";
    }
}
