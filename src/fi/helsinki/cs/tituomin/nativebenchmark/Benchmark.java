package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;

public interface Benchmark extends Runnable {
    public String from();
    public String to();
}