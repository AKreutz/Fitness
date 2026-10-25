import Toybox.Lang;
import Toybox.WatchUi;

// START does the main thing for wherever the workout is: finishes a set, starts the next one, or confirms the
// highlighted answer. UP and DOWN move the highlight while rating or answering the weight offer, and holding UP (the
// menu gesture) skips the exercise. BACK leaves the app.
class FitnessWatchDelegate extends WatchUi.BehaviorDelegate {
    private var mModel as SessionModel;
    private var mLink as PhoneLink;

    function initialize(model as SessionModel, link as PhoneLink) {
        BehaviorDelegate.initialize();
        mModel = model;
        mLink = link;
    }

    function onSelect() as Boolean {
        if (!mModel.hasState) {
            mLink.sync();
            return true;
        }
        if (mModel.phase == PHASE_FINISHED || mModel.phase == PHASE_UNAVAILABLE) {
            return true;
        }
        // The phone changes the state when it takes the command, not here, so wait for it before sending another.
        if (!mModel.takeCommandTurn()) {
            return true;
        }
        if (mModel.phase == PHASE_RATE) {
            mLink.rate(mModel.selection);
        } else if (mModel.phase == PHASE_WEIGHT_OFFER) {
            mLink.answerWeightOffer(mModel.selection == 0);
        } else {
            mLink.advance();
        }
        return true;
    }

    function onMenu() as Boolean {
        if (mModel.isActive() && !mModel.isPrompt() && mModel.takeCommandTurn()) {
            mLink.skip();
        }
        return true;
    }

    function onNextPage() as Boolean {
        return moveSelection(1);
    }

    function onPreviousPage() as Boolean {
        return moveSelection(-1);
    }

    private function moveSelection(step as Number) as Boolean {
        if (mModel.isPrompt()) {
            mModel.moveSelection(step);
            WatchUi.requestUpdate();
        }
        return true;
    }
}
