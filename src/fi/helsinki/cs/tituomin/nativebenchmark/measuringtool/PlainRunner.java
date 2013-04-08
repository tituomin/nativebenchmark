package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.io.IOException;
import android.os.SystemClock;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import android.util.Log;

public class PlainRunner extends MeasuringTool {

    public PlainRunner(int i) throws IOException, InterruptedException {
        super(i);
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        // no options needed, time is returned as is (not in extra file)
        return options;
    }

    public boolean explicitGC() {
        return false;
    }

    public void start(Benchmark benchmark)
    throws InterruptedException, IOException {
        benchmark.setRepetitions(3000);
        benchmark.run();
        benchmark.restoreRepetitions();
    }
}
