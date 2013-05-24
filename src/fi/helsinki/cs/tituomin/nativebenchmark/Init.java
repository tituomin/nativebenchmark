package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.ShellEnvironment;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class Init {

    public static final int CPUFREQ     =  400000;
    public static final int CPUFREQ_MAX = 1000000;

    public static List<String> initScript(int cpufreq) {
        List <String> script = new ArrayList<String> ();
        script.add(
            "echo \"userspace\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
        script.add(
            "echo \"" + cpufreq +  "\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed");
        return script;

    }

    private static boolean initDone = false;
    public static void initEnvironment(boolean maxSpeed) throws IOException {
        if (!initDone) {
            ShellEnvironment.runAsRoot(initScript(maxSpeed ? CPUFREQ_MAX : CPUFREQ));
            initDone = true;
        }
    }
}
