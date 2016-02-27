package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;

import android.os.Process;

public abstract class Benchmark implements Runnable {
    protected abstract void runInternal();
    public abstract String from();
    public abstract String to();
    public abstract String description();
    public abstract String id();
    public abstract int sequenceNo();
    public abstract boolean isAllocating();
    public abstract boolean isNonvirtual();
    public abstract boolean dynamicParameters();
    public abstract boolean representative();

    public long repetitionsLeft;

    public void run() {
        Process.setThreadPriority(-5);
        runInternal();
    }

    protected BenchmarkParameter benchmarkParameter;
    protected long repetitions;

    public void init(BenchmarkParameter bp) {
        repetitionsLeft = 0;
        benchmarkParameter = bp;
        repetitions = -1;
    }

    public void setRepetitions(long reps) {
        if (reps < 1) {
            return;
        }
        repetitions = reps;
        BenchmarkRegistry.setRepetitions(reps);
    }
}
