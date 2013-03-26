package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.io.IOException;
import android.os.SystemClock;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import android.util.Log;

public class ResponseTimeRecorder extends MeasuringTool {

    public ResponseTimeRecorder(int i) {
        super(i);
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        // no options needed, time is returned as is (not in extra file)
        return options;
    }

    public Measurement start(Benchmark benchmark)
    throws InterruptedException, IOException {
        long endTime = 0, startTime = SystemClock.uptimeMillis();
        benchmark.run();
        // todo mainitse tekstissä toteutus (ks. todo.org)
        endTime = SystemClock.uptimeMillis();
        // todo: epätarkkuus, init ym. ? 
        measurement.addData("response_time_millis", "" + (endTime - startTime));
        return measurement;
    }

}
