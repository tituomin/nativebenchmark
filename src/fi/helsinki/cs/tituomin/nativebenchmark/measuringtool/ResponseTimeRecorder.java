package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.io.IOException;
import android.os.SystemClock;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import android.util.Log;

public class ResponseTimeRecorder extends MeasuringTool {

    public ResponseTimeRecorder(int i, long reps, long allocreps, boolean warmup) throws IOException, InterruptedException {
        super(i, reps, allocreps);
        this.warmup = warmup;
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        // no options needed, time is returned as is (not in extra file)
        return options;
    }

    public boolean explicitGC() {
        return !warmup;
    }

    public boolean ignore() {
        return warmup;
    }

    public void start(Runnable benchmark)
    throws InterruptedException, IOException {
        long endTime, startTime;
        startTime = SystemClock.uptimeMillis();
        benchmark.run();
        endTime = SystemClock.uptimeMillis();
        String delta = "" + (endTime - startTime);
        putMeasurement("response_time", delta);
        putMeasurement("time_unit", "milliseconds");
    }

    private boolean warmup;
}
