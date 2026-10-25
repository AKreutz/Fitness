import Toybox.Lang;

module Format {
    // A weight without trailing zeros: 80, 62.5, 1.25.
    function weight(kg as Float) as String {
        var hundredths = (kg * 100 + 0.5).toNumber();
        var whole = hundredths / 100;
        var fraction = hundredths % 100;
        if (fraction == 0) {
            return whole.toString();
        }
        if (fraction % 10 == 0) {
            return whole + "." + (fraction / 10);
        }
        return whole + "." + fraction.format("%02d");
    }
}
