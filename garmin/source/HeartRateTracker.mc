import Toybox.Activity;
import Toybox.ActivityRecording;
import Toybox.FitContributor;
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
    // The ids of the lap fields in resources/fitfields/fitfields.xml, and the room for an exercise name in the file.
    private const FIELD_EXERCISE = 0;
    private const FIELD_WEIGHT = 1;
    private const FIELD_REPS = 2;
    private const FIELD_SET = 3;
    private const EXERCISE_NAME_BYTES = 32;

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
    private var mLapKey as String = "";
    private var mExerciseField as FitContributor.Field? = null;
    private var mWeightField as FitContributor.Field? = null;
    private var mRepsField as FitContributor.Field? = null;
    private var mSetField as FitContributor.Field? = null;

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
            lapOnPhase(model);
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
        var lap = FitContributor.MESG_TYPE_LAP;
        mExerciseField = session.createField("exercise", FIELD_EXERCISE, FitContributor.DATA_TYPE_STRING,
            {:mesgType => lap, :count => EXERCISE_NAME_BYTES});
        mWeightField = session.createField("weight", FIELD_WEIGHT, FitContributor.DATA_TYPE_FLOAT,
            {:mesgType => lap, :units => "kg"});
        mRepsField = session.createField("reps", FIELD_REPS, FitContributor.DATA_TYPE_UINT8, {:mesgType => lap});
        mSetField = session.createField("set", FIELD_SET, FitContributor.DATA_TYPE_UINT8, {:mesgType => lap});
        session.start();
        mActivity = session;
        mLapKey = "";
    }

    // Gives every working set, and every stretch of warm-up or rest around them, a lap of its own, labelled with the
    // exercise, weight, target reps and set number of the set (the label alone for a stretch that is not a set).
    private function lapOnPhase(model as SessionModel) as Void {
        var session = mActivity;
        if (session == null) {
            return;
        }
        var working = model.phase == PHASE_WORKING;
        var key = working ? model.exerciseId + ":" + model.setNumber
            : (model.phase == PHASE_WARM_UP ? "warm-up" : "rest");
        if (key.equals(mLapKey)) {
            return;
        }
        if (mLapKey.length() > 0) {
            session.addLap();
        }
        mLapKey = key;
        var name = working ? model.exerciseName
            : WatchUi.loadResource(model.phase == PHASE_WARM_UP ? Rez.Strings.WarmUp : Rez.Strings.Rest) as String;
        var exerciseField = mExerciseField;
        var weightField = mWeightField;
        var repsField = mRepsField;
        var setField = mSetField;
        if (exerciseField == null || weightField == null || repsField == null || setField == null) {
            return;
        }
        exerciseField.setData(name);
        weightField.setData(working ? model.weightKg : 0.0);
        repsField.setData(working ? model.targetReps : 0);
        setField.setData(working ? model.setNumber : 0);
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
