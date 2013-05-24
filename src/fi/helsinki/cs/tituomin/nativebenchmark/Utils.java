package fi.helsinki.cs.tituomin.nativebenchmark;
import java.util.UUID;

public class Utils {

    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    public static String humanTime(long millis) {
        String time;
        long seconds = millis  / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long seconds_total = seconds;
        seconds %= 60;
        minutes %= 60;
        return (
            hours   + "h " +
            minutes + "m " +
            seconds + "s " +
            "(" + seconds_total + " s tot.)");
    }


}
