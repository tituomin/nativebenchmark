package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;

import java.io.IOException;

public class RunningWrapper implements Runnable {
    private Benchmark benchmark;
    private Exception exceptionThrown;
    private boolean interrupted;
    private long repetitions;

    public RunningWrapper(Benchmark b) {
        benchmark = b;
    }

    protected void setException(Exception e) {
        exceptionThrown = e;
    }

    protected void setInterrupted() {
        interrupted = true;
    }

    public boolean exceptionWasThrown() {
        return exceptionThrown != null;
    }

    public void setRepetitions(long r) {
        repetitions = r;
    }

    public Benchmark getBenchmark() {
        return benchmark;
    }

    public long getRepetitions() {
        return repetitions;
    }

    public boolean wasInterrupted() {
        return interrupted;
    }

    public Exception getException() {
        return exceptionThrown;
    }

    public void init(Benchmark benchmark) {
        interrupted = false;
        exceptionThrown = null;
    }

    public void run() {
        benchmark.run();
    }
    
    public void begin(MeasuringTool tool)  throws InterruptedException, IOException {
        init(benchmark);
        tool.start(this);
        if (Thread.currentThread().isInterrupted()) {
            setInterrupted();
        }
    }

}
