import Toybox.Application;
import Toybox.Lang;
import Toybox.WatchUi;

// Companion for the Fitness Android app: shows the current exercise and logs sets from the wrist. The phone owns the
// workout data; this app shows it and sends back what happened.
class FitnessWatchApp extends Application.AppBase {
    function initialize() {
        AppBase.initialize();
    }

    function getInitialView() as [WatchUi.Views] or [WatchUi.Views, WatchUi.InputDelegates] {
        return [new FitnessWatchView()];
    }
}
