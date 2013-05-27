package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.io.IOException;
import android.os.SystemClock;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import android.util.Log;

public class JavaSystemNanoResponseTimeRecorder extends ResponseTimeRecorder {

    public JavaSystemNanoResponseTimeRecorder(int i, long reps, long allocreps, boolean warmup) throws IOException, InterruptedException {
        super(i, reps, allocreps, warmup);
    }

    public void start(Runnable benchmark)
    throws InterruptedException, IOException {
        long endTime, startTime;
        startTime = System.nanoTime();
        benchmark.run();
        endTime = System.nanoTime();
        String delta = "" + (endTime - startTime);
        putMeasurement("response_time", delta);
        putMeasurement("time_unit", "nanoseconds");
    }
}
