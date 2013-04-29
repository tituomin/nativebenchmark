package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.AllocatingBenchmarkWrapper;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import java.io.IOException;

public class AllocatingBenchmarkLongRunningWrapper extends AllocatingBenchmarkWrapper {

    public AllocatingBenchmarkLongRunningWrapper(Benchmark b) {
        super(b);
    }

    private long toolRepetitions;

    public void begin(MeasuringTool tool)  throws InterruptedException, IOException {
        Benchmark benchmark = getBenchmark();
        init(benchmark);
        toolRepetitions  = tool.repetitions();
        tool.putMeasurement("repetitions", benchmark.maxRepetitions() + "");
        tool.start(this);
        tool.finishMeasurement();
    }

    public void run() {
        // This method ensures the garbage collector is run
        // every benchmark.maxrepetitions iteration
        // but otherwise the measurement is
        // run for a long period
        Benchmark benchmark = getBenchmark();
        long interval = BenchmarkRegistry.CHECK_INTERRUPTED_INTERVAL;
        long division, remainder;
        long repetitions = toolRepetitions;

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
