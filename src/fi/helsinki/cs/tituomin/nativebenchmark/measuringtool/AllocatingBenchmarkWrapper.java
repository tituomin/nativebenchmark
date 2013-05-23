package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;

public class AllocatingBenchmarkWrapper extends RunningWrapper {

    public AllocatingBenchmarkWrapper(Benchmark b, long repetitions) {
        super(b);
        this.repetitions = repetitions;
    }

    public static final int GC_PAUSE_MS = 400;

    public void init(Benchmark benchmark) {
        super.init(benchmark);
        benchmark.setRepetitions(repetitions);
    }

    protected long repetitions;

}
