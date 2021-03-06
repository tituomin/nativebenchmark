package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.AllocatingBenchmarkWrapper;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import java.io.IOException;

public class AllocatingBenchmarkShortRunningWrapper extends AllocatingBenchmarkWrapper {

    public AllocatingBenchmarkShortRunningWrapper(Benchmark b, long r) {
        super(b, r);
    }

    private static final long MULTIPLIER = 25;

    public void begin(MeasuringTool tool) throws InterruptedException, IOException {
        Benchmark benchmark = getBenchmark();
        init(benchmark);
        long reps = MULTIPLIER;
        reps += 1;
        while (--reps != 0) {
            tool.putMeasurement("repetitions", this.repetitions + "");
            tool.putMeasurement("multiplier", MULTIPLIER + "");
            try {
                tool.start(getBenchmark());
                tool.finishMeasurement();
                System.gc();
                Thread.sleep(GC_PAUSE_MS);
            }
            catch (InterruptedException e) {
                setInterrupted();
                return;
            }
            catch (Exception e) {
                setException(e);
                return;
            }
        }
    }
}
