package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.AllocatingBenchmarkWrapper;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import java.io.IOException;

public class AllocatingBenchmarkLongRunningWrapper extends AllocatingBenchmarkWrapper {

    public AllocatingBenchmarkLongRunningWrapper(Benchmark b, long r) {
        super(b, r);
    }

    private static final long MAX_REPS = Long.MAX_VALUE;

    public void begin(MeasuringTool tool)  throws InterruptedException, IOException {
        Benchmark benchmark = getBenchmark();
        init(benchmark);
        tool.putMeasurement("repetitions", this.repetitions + "");
        tool.start(this);
        tool.finishMeasurement();
    }

    public void run() {
        // This method ensures the garbage collector is run
        // every benchmark.maxrepetitions iteration
        // but otherwise the measurement is
        // run for a period long enough for profiling.
        Benchmark benchmark = getBenchmark();
        long interval = BenchmarkRegistry.CHECK_INTERRUPTED_INTERVAL;
        long division, remainder;
        long repetitions = MAX_REPS;

        division  = repetitions / interval + 1;
        remainder = repetitions % interval + 1;

        while (--division != 0) { 
            interval = BenchmarkRegistry.CHECK_INTERRUPTED_INTERVAL + 1;
            while (--interval != 0) { 
                try {
                    benchmark.run();
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
            if (Thread.currentThread().isInterrupted()) {
                setInterrupted();
                return;
            }
        }

        while (--remainder != 0) { 
            try {
                benchmark.run();
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
