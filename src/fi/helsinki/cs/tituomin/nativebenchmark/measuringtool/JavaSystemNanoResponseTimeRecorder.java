package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.io.IOException;
import android.os.SystemClock;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import android.util.Log;

public class JavaSystemNanoResponseTimeRecorder extends ResponseTimeRecorder {

    public JavaSystemNanoResponseTimeRecorder(int i, long reps, long allocreps) throws IOException, InterruptedException {
        super(i, reps, allocreps);
    }

    public void start(Runnable benchmark)
    throws InterruptedException, IOException {
        long endTime = 0, startTime = System.nanoTime();
        benchmark.run();
        // todo mainitse tekstissä toteutus (ks. todo.org)
        endTime = System.nanoTime();
        // todo: epätarkkuus, init ym. ?
        String delta = "" + (endTime - startTime);
        putMeasurement("response_time_nanos", delta);
    }
}
