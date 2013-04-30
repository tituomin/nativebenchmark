package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.ShellEnvironment;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class Init {

    public static final String CPUFREQ = "400000";
    public final static List<String> INIT_ENVIRONMENT;

    static {
        INIT_ENVIRONMENT = new ArrayList<String> ();
        INIT_ENVIRONMENT.add(
            "echo \"userspace\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
        INIT_ENVIRONMENT.add(
            "echo \"" + CPUFREQ +  "\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed");
    }

    public static void initEnvironment() throws IOException {
        if (!initDone) {
            ShellEnvironment.execute(INIT_ENVIRONMENT);
            initDone = true;
        }
    }

    private static boolean initDone = false;

}
