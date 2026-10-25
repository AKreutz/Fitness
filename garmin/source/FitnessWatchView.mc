import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

// Shows where the workout is, one screen per phase. Sized for the Forerunner 745's round 240x240 screen, so text sits
// in the middle where the circle is widest.
class FitnessWatchView extends WatchUi.View {
    private const CENTER = Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER;
    private const EFFORT_COLORS = [Graphics.COLOR_GREEN, Graphics.COLOR_YELLOW, Graphics.COLOR_RED];
    // Dark enough for the pure green, yellow and red answers to read on it.
    private const SELECTED_BACKGROUND = 0x000055;

    private var mModel as SessionModel;
    private var mTracker as HeartRateTracker;

    function initialize(model as SessionModel, tracker as HeartRateTracker) {
        View.initialize();
        mModel = model;
        mTracker = tracker;
    }

    function onUpdate(dc as Graphics.Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();
        var w = dc.getWidth();
        var h = dc.getHeight();

        if (!mModel.hasState) {
            var message = mModel.phoneReachable ? Rez.Strings.Waiting : Rez.Strings.NoPhone;
            drawFitted(dc, w / 2, h / 2, text(message), w * 0.8);
            return;
        }

        switch (mModel.phase) {
            case PHASE_WARM_UP:
                drawExercise(dc, w, h, text(Rez.Strings.WarmUp), text(Rez.Strings.StartFirstSet));
                break;
            case PHASE_WORKING:
                drawExercise(dc, w, h, text(Rez.Strings.Set) + " " + mModel.setNumber + " / " + mModel.setCount,
                    text(Rez.Strings.FinishSet));
                break;
            case PHASE_RESTING:
                drawResting(dc, w, h);
                break;
            case PHASE_RATE:
                drawRating(dc, w, h);
                break;
            case PHASE_WEIGHT_OFFER:
                drawWeightOffer(dc, w, h);
                break;
            case PHASE_FINISHED:
                drawFinished(dc, w, h);
                break;
            default:
                drawFitted(dc, w / 2, h / 2, text(Rez.Strings.Unavailable), w * 0.8);
                break;
        }
        if (!mModel.phoneReachable) {
            dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, h * 0.045, Graphics.FONT_XTINY, text(Rez.Strings.NoPhoneBanner), CENTER);
        }
    }

    // The exercise with what to do now ("Set 2 / 4"), its weight and reps, and a hint for START.
    private function drawExercise(dc as Graphics.Dc, w as Number, h as Number, status as String, hint as String) as Void {
        drawHeader(dc, w, h);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.43, Graphics.FONT_SMALL, status, CENTER);
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.6, Graphics.FONT_MEDIUM, weightAndReps(), CENTER);
        drawHint(dc, w, h * 0.79, hint);
        drawHeartRate(dc, w, h);
    }

    private function drawResting(dc as Graphics.Dc, w as Number, h as Number) as Void {
        drawHeader(dc, w, h);
        var elapsed = mModel.restElapsedSeconds();
        var goal = mModel.restGoalSeconds;
        dc.setColor(elapsed >= goal ? Graphics.COLOR_GREEN : Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.48, Graphics.FONT_NUMBER_HOT, clock(elapsed), CENTER);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.68, Graphics.FONT_XTINY, text(Rez.Strings.Rest) + " " + clock(goal), CENTER);
        drawHint(dc, w, h * 0.79, text(Rez.Strings.NextSet));
        drawHeartRate(dc, w, h);
    }

    private function drawRating(dc as Graphics.Dc, w as Number, h as Number) as Void {
        drawHeader(dc, w, h);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.35, Graphics.FONT_XTINY, text(Rez.Strings.HowDidItFeel), CENTER);
        var labels = [text(Rez.Strings.Easy), text(Rez.Strings.Medium), text(Rez.Strings.Hard)];
        for (var i = 0; i < labels.size(); i++) {
            drawChoice(dc, w, h * (0.43 + i * 0.125), h * 0.11, labels[i], i == mModel.selection, EFFORT_COLORS[i]);
        }
        drawHint(dc, w, h * 0.87, text(Rez.Strings.Confirm));
    }

    private function drawWeightOffer(dc as Graphics.Dc, w as Number, h as Number) as Void {
        drawHeader(dc, w, h);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.35, Graphics.FONT_XTINY, text(Rez.Strings.RaiseWeight), CENTER);
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.47, Graphics.FONT_SMALL,
            Format.weight(mModel.weightKg) + " > " + Format.weight(mModel.offeredWeightKg) + " kg", CENTER);
        drawChoice(dc, w, h * 0.57, h * 0.11, text(Rez.Strings.Yes), mModel.selection == 0, Graphics.COLOR_GREEN);
        drawChoice(dc, w, h * 0.7, h * 0.11, text(Rez.Strings.No), mModel.selection == 1, Graphics.COLOR_LT_GRAY);
        drawHint(dc, w, h * 0.87, text(Rez.Strings.Confirm));
    }

    // The last exercise is rated, and the phone waits for the heart rate readings before it saves the workout.
    private function drawFinishing(dc as Graphics.Dc, w as Number, h as Number) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.38, Graphics.FONT_MEDIUM, text(Rez.Strings.Finishing), CENTER);
        var label = Rez.Strings.SavingWorkout;
        var color = Graphics.COLOR_LT_GRAY;
        if (mModel.heartRateStatus == HEART_RATE_SENDING) {
            label = Rez.Strings.HeartRateSending;
        } else if (mModel.heartRateStatus == HEART_RATE_FAILED) {
            label = Rez.Strings.HeartRateNotSent;
            color = Graphics.COLOR_RED;
        }
        dc.setColor(color, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.56, Graphics.FONT_XTINY, text(label), CENTER);
    }

    private function drawFinished(dc as Graphics.Dc, w as Number, h as Number) as Void {
        if (!mModel.workoutSaved) {
            drawFinishing(dc, w, h);
            return;
        }
        dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.28, Graphics.FONT_MEDIUM, text(Rez.Strings.WorkoutDone), CENTER);
        var y = h * 0.45;
        if (mModel.personalRecords > 0) {
            dc.setColor(Graphics.COLOR_YELLOW, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, y, Graphics.FONT_SMALL,
                mModel.personalRecords + " " + text(Rez.Strings.PersonalRecords), CENTER);
            y += h * 0.13;
        }
        if (mTracker.averageBpm() > 0) {
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, y, Graphics.FONT_SMALL,
                mTracker.averageBpm() + " " + text(Rez.Strings.AverageHeartRate), CENTER);
        }
        drawHeartRateStatus(dc, w, h * 0.71);
        drawHint(dc, w, h * 0.85, text(Rez.Strings.LeaveHint));
    }

    // Whether the readings were saved with the workout, which this watch sent them for.
    private function drawHeartRateStatus(dc as Graphics.Dc, w as Number, y as Numeric) as Void {
        if (mModel.heartRateStatus == HEART_RATE_SAVED) {
            dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, y, Graphics.FONT_XTINY, text(Rez.Strings.HeartRateSaved), CENTER);
        } else if (mModel.heartRateStatus == HEART_RATE_FAILED) {
            dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, y, Graphics.FONT_XTINY, text(Rez.Strings.HeartRateNotSent), CENTER);
        }
    }

    // The workout's progress and the exercise's name.
    private function drawHeader(dc as Graphics.Dc, w as Number, h as Number) as Void {
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.12, Graphics.FONT_XTINY, mModel.exerciseNumber + " / " + mModel.exerciseCount, CENTER);
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        drawFitted(dc, w / 2, h * 0.24, mModel.exerciseName, w * 0.78);
    }

    private function drawHint(dc as Graphics.Dc, w as Number, y as Numeric, hint as String) as Void {
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, y, Graphics.FONT_XTINY, hint, CENTER);
    }

    private function drawHeartRate(dc as Graphics.Dc, w as Number, h as Number) as Void {
        var bpm = mTracker.currentBpm;
        if (bpm == null || bpm <= 0) {
            return;
        }
        dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.9, Graphics.FONT_XTINY, bpm + " " + text(Rez.Strings.HeartRate), CENTER);
    }

    // One answer of a list, with the chosen one on a filled background.
    private function drawChoice(dc as Graphics.Dc, w as Number, y as Float, height as Float, label as String,
            selected as Boolean, color as Number) as Void {
        var left = w * 0.22;
        var middle = y + height / 2;
        if (selected) {
            dc.setColor(SELECTED_BACKGROUND, Graphics.COLOR_TRANSPARENT);
            dc.fillRoundedRectangle(left, y, w - 2 * left, height, 10);
        }
        dc.setColor(selected ? color : Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, middle, Graphics.FONT_SMALL, label, CENTER);
    }

    // Draws in the biggest of a few sizes that fits, so long exercise names stay on the screen.
    private function drawFitted(dc as Graphics.Dc, x as Numeric, y as Numeric, label as String, maxWidth as Numeric) as Void {
        var fonts = [Graphics.FONT_MEDIUM, Graphics.FONT_SMALL, Graphics.FONT_XTINY];
        var chosen = fonts[fonts.size() - 1];
        for (var i = 0; i < fonts.size(); i++) {
            if (dc.getTextWidthInPixels(label, fonts[i]) <= maxWidth) {
                chosen = fonts[i];
                break;
            }
        }
        dc.drawText(x, y, chosen, label, CENTER);
    }

    private function weightAndReps() as String {
        return Format.weight(mModel.weightKg) + " kg x " + mModel.targetReps;
    }

    private function clock(seconds as Number) as String {
        return (seconds / 60) + ":" + (seconds % 60).format("%02d");
    }

    private function text(resource as Lang.ResourceId) as String {
        return WatchUi.loadResource(resource) as String;
    }
}
