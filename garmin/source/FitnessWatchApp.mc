import Toybox.Application;
import Toybox.Attention;
import Toybox.Communications;
import Toybox.Lang;
import Toybox.Timer;
import Toybox.WatchUi;

// Companion for the Fitness Android app: shows the workout the phone is running and lets you work through it from the
// wrist. The phone owns the session; this app shows it, sends commands, and measures heart rate.
class FitnessWatchApp extends Application.AppBase {
    private const TICK_MILLIS = 1000;
    // How many ticks pass between asking the phone for its state while it has not answered yet.
    private const SYNC_EVERY_TICKS = 5;

    private var mModel as SessionModel = new SessionModel();
    private var mLink as PhoneLink = new PhoneLink(mModel);
    private var mTracker as HeartRateTracker = new HeartRateTracker();
    private var mSender as HeartRateSender = new HeartRateSender(mModel, mLink);
    private var mTimer as Timer.Timer? = null;
    private var mTicks as Number = 0;
    private var mRestAlerted as Boolean = false;

    function initialize() {
        AppBase.initialize();
    }

    function getInitialView() as [WatchUi.Views] or [WatchUi.Views, WatchUi.InputDelegates] {
        mTracker.start();
        mLink.setHeartRateSender(mSender);
        Communications.registerForPhoneAppMessages(method(:onPhoneMessage));
        // Ask for the current state. The phone also pushes every change by itself, but only once a workout is open
        // there, so keep asking until it answers.
        mLink.sync();
        var timer = new Timer.Timer();
        timer.start(method(:onTick), TICK_MILLIS, true);
        mTimer = timer;
        return [new FitnessWatchView(mModel, mTracker), new FitnessWatchDelegate(mModel, mLink)];
    }

    function onPhoneMessage(message as Communications.PhoneAppMessage) as Void {
        var data = message.data;
        if (!(data instanceof Dictionary)) {
            return;
        }
        var wasFollowing = mModel.hasState;
        var changed = mModel.update(data);
        if (wasFollowing && !mModel.hasState) {
            // The phone signed off: forget the workout, its readings, and any sending of them.
            mTracker.reset();
            mSender.cancel();
        }
        mTracker.follow(mModel);
        if (changed) {
            buzz(50, 80);
        }
        if (mModel.phase == PHASE_FINISHED && mModel.sessionId.length() > 0 && !mModel.workoutSaved) {
            // The workout is done and the phone is waiting for the heart rate readings before it saves it. This is the
            // one time they are sent: all of them, once, so the link is free for the phone's answers during the workout.
            mSender.begin(mModel.sessionId, mTracker);
        }
        WatchUi.requestUpdate();
    }

    function onTick() as Void {
        mTicks++;
        mTracker.sample();
        mSender.tick();
        if (!mModel.hasState && mTicks % SYNC_EVERY_TICKS == 0) {
            mLink.sync();
        }
        if (mModel.phase == PHASE_RESTING) {
            if (!mRestAlerted && mModel.restElapsedSeconds() >= mModel.restGoalSeconds) {
                mRestAlerted = true;
                buzz(100, 600);
            }
            WatchUi.requestUpdate();
        } else {
            mRestAlerted = false;
        }
    }

    function onStop(state as Dictionary?) as Void {
        if (mTimer != null) {
            mTimer.stop();
            mTimer = null;
        }
        mTracker.stop();
        Communications.registerForPhoneAppMessages(null);
    }

    private function buzz(strength as Number, millis as Number) as Void {
        if (Attention has :vibrate) {
            Attention.vibrate([new Attention.VibeProfile(strength, millis)]);
        }
    }
}
