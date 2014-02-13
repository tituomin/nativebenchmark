package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkInitialiser;
import fi.helsinki.cs.tituomin.nativebenchmark.benchmark.JavaCounterparts;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

public class BenchmarkRegistry {

    private static List<Benchmark> benchmarks;

    public static long repetitions;
    public static final long CHECK_INTERRUPTED_INTERVAL = 1000;

    public static List<Benchmark> getBenchmarks() {
        return benchmarks;
    }

    public static void init(long reps) throws ClassNotFoundException {
        repetitions = reps;
        benchmarks = new LinkedList<Benchmark> ();
        Class jCounterparts = Class.forName("fi.helsinki.cs.tituomin.nativebenchmark.benchmark.JavaCounterparts");
        Class threadClass = Class.forName("java.lang.Thread");
        initNative(reps, CHECK_INTERRUPTED_INTERVAL, jCounterparts, JavaCounterparts.INSTANCE, threadClass);
    }

    public static native void initNative(long repetitions, long interval, Class javaCounterparts, JavaCounterparts counterInstance, Class threadClass);
    public static native void setRepetitions(long repetitions);
    public static native void interruptNative();
}
