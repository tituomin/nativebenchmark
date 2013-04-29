package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;

public class AllocatingBenchmarkWrapper extends RunningWrapper {

    public AllocatingBenchmarkWrapper(Benchmark b) {
        super(b);
    }

    public static final int GC_PAUSE_MS = 200;

    public void init(Benchmark benchmark) {
        super.init(benchmark);
        benchmark.setRepetitions(benchmark.maxRepetitions());
    }

}
