import Toybox.Activity;
import Toybox.ActivityRecording;
import Toybox.Lang;
import Toybox.Sensor;
import Toybox.System;
import Toybox.Time;
import Toybox.WatchUi;

// Measures the wearer's heart rate. While the phone's workout is running it keeps one reading for every second of it
// (0 for a second the sensor had none), to be sent to the phone once the workout is finished. It also records the
// workout as a strength training activity, with a lap per exercise, that the watch saves and syncs to Garmin Connect.
class HeartRateTracker {
    // A workout longer than this stops being recorded, so the readings cannot fill the watch's memory.
    private const MAX_SECONDS = 43200;

    // The latest reading, or null while the sensor has none.
    public var currentBpm as Number? = null;
    // One byte per second since the recording began, each the heart rate in beats per minute, 0 for none.
    public var samples as ByteArray = []b;
    // When the recording began, in epoch seconds, so the phone can line the readings up with its own clock.
    public var startEpochSeconds as Long = 0l;
    private var mRecording as Boolean = false;
    private var mFinished as Boolean = false;
    private var mStartedAtMillis as Number = 0;
    private var mAverage as Number = 0;
    private var mActivity as ActivityRecording.Session? = null;
    private var mLapExerciseId as String = "";

    function start() as Void {
        Sensor.setEnabledSensors([Sensor.SENSOR_HEARTRATE]);
        Sensor.enableSensorEvents(method(:onSensor));
    }

    function stop() as Void {
        discardActivity();
        Sensor.enableSensorEvents(null);
        Sensor.setEnabledSensors([]);
    }

    // Follows the phone: readings are kept while a workout is running. A workout that is finished stops the recording
    // but keeps what it holds until it has been sent, or a new workout starts.
    function follow(model as SessionModel) as Void {
        if (model.phase == PHASE_FINISHED && model.hasState) {
            if (mRecording) {
                mRecording = false;
                mFinished = true;
                mAverage = computeAverage();
                saveActivity();
            }
        } else if (model.isActive() && !mRecording) {
            if (mFinished) {
                // A new workout, with the finished one's readings no longer needed.
                reset();
            }
            if (samples.size() == 0) {
                startEpochSeconds = Time.now().value().toLong();
                mStartedAtMillis = System.getTimer();
            }
            mRecording = true;
            startActivity();
        }
        if (mRecording) {
            lapOnNewExercise(model.exerciseId);
        }
    }

    // Takes this second's reading. Called once a second; a second that was missed is filled in as having no reading.
    function sample() as Void {
        if (!mRecording) {
            return;
        }
        var second = (System.getTimer() - mStartedAtMillis) / 1000;
        if (second >= MAX_SECONDS) {
            return;
        }
        while (samples.size() < second) {
            samples.add(0);
        }
        if (samples.size() == second) {
            var bpm = currentBpm;
            samples.add(bpm != null && bpm > 0 && bpm < 256 ? bpm : 0);
        }
    }

    // The average of the readings of the finished workout, or 0 if there are none.
    function averageBpm() as Number {
        return mAverage;
    }

    // Forgets every reading, for the next workout.
    function reset() as Void {
        discardActivity();
        samples = []b;
        startEpochSeconds = 0l;
        mRecording = false;
        mFinished = false;
        mAverage = 0;
    }

    function onSensor(info as Sensor.Info) as Void {
        currentBpm = info.heartRate;
        WatchUi.requestUpdate();
    }

    private function startActivity() as Void {
        if (mActivity != null || !(ActivityRecording has :createSession)) {
            return;
        }
        var session = ActivityRecording.createSession({
            :name => "Strength",
            :sport => Activity.SPORT_TRAINING,
            :subSport => Activity.SUB_SPORT_STRENGTH_TRAINING
        });
        session.start();
        mActivity = session;
        mLapExerciseId = "";
    }

    // Marks a lap when the workout moves on to another exercise.
    private function lapOnNewExercise(exerciseId as String) as Void {
        var session = mActivity;
        if (session == null || exerciseId.length() == 0 || exerciseId.equals(mLapExerciseId)) {
            return;
        }
        if (mLapExerciseId.length() > 0) {
            session.addLap();
        }
        mLapExerciseId = exerciseId;
    }

    // Ends the activity of a finished workout and keeps it, for the watch to sync to Garmin Connect.
    private function saveActivity() as Void {
        var session = mActivity;
        mActivity = null;
        if (session != null) {
            session.stop();
            session.save();
        }
    }

    // Throws away an activity that never finished: the phone left the workout, or the app was closed during it.
    private function discardActivity() as Void {
        var session = mActivity;
        mActivity = null;
        if (session != null) {
            session.stop();
            session.discard();
        }
    }

    private function computeAverage() as Number {
        var sum = 0;
        var count = 0;
        for (var i = 0; i < samples.size(); i++) {
            if (samples[i] > 0) {
                sum += samples[i];
                count++;
            }
        }
        return count == 0 ? 0 : (sum + count / 2) / count;
    }
}
