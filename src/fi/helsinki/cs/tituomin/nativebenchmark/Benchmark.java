package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;

public abstract class Benchmark implements Runnable {
    public abstract void run();
    public abstract String from();
    public abstract String to();
    public abstract String description();
    public abstract String id();
    public abstract int sequenceNo();
    public abstract long maxRepetitions();
    public abstract boolean dynamicParameters();

    public long repetitionsLeft;
    public int persistentValue;

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
