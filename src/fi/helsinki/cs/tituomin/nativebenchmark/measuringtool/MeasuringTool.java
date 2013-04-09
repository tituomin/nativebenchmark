package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import android.util.Log;
import java.util.Observer;
import java.util.Observable;
import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkResult;

public abstract class MeasuringTool implements Runnable {

    public MeasuringTool()  throws IOException, InterruptedException {
        this(1);
    }

    public MeasuringTool(int rounds) throws IOException, InterruptedException {
        specifyOptions();
        hasOptions = false;
        this.rounds = rounds;
        this.measurement = new BenchmarkResult();
        init();
    }

    protected void init() throws IOException, InterruptedException { }

    protected abstract void start(Benchmark benchmark)
        throws InterruptedException, IOException;

    public void startMeasuring(Benchmark benchmark) throws InterruptedException, IOException {
        clearMeasurement();
        this.hasOptions = false;
        start(benchmark);
    }

    public void run() {
        try {
            start(benchmark);
        }
        catch (InterruptedException e) {
            Log.e("BenchmarkRunner", "Measuring was interrupted ", e);
        }
        catch (IOException e) {
            Log.e("BenchmarkRunner", "IOException", e);
        }
    }

    public boolean explicitGC() {
        return true;
    }

    protected abstract List<MeasuringOption>
        defaultOptions(List<MeasuringOption> container);

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> container) {
        return container;
    }

    protected void specifyOptions() {
        this.allowedOptions = specifyAllowedOptions(
            new LinkedList<OptionSpec>());

        if (this.requiredOptions == null) {
            this.requiredOptions = new HashSet<OptionSpec>();
        }
        for (OptionSpec o : this.allowedOptions) {
            if (o.required) {
                this.requiredOptions.add(o);
            }
        }
        for (MeasuringOption op : defaultOptions(
                 new LinkedList<MeasuringOption>())) {
            setOption(op);
        };
    }

    // -----

    public MeasuringTool set(OptionSpec spec, String value) {
        // todo: not typesafe (assumes basicoption)
        setOption(new BasicOption(spec, value));
        return this;
    }

    public String getOption(OptionSpec id) {
        return this.options.get(id).value();
    }

    public void setOption(MeasuringOption option) {
        if (this.allowedOptions == null) {
            specifyOptions();
        }
        if (!allowedOptions.contains(option.type())) {
            throw new UnsupportedOptionException();
        }
        else {
            this.options =
                options != null ?
                options :
                new HashMap<OptionSpec,MeasuringOption> ();
            
            this.options.put(option.type(), option);
        }
    }

    public void setBenchmark(Benchmark b) {
        benchmark = b;
    }


    public void addObserver(ApplicationState o) {
        if (this.observers == null) {
            this.observers = new LinkedList<ApplicationState>();
        }
        this.observers.add(o);
    }

    public void notifyObservers(ApplicationState.State state) {
        
        for (ApplicationState o : this.observers) {
            o.updateState(state);
        }
    }
    public final static Observable mockObservable = new Observable();

    private boolean hasRequiredOptions() {
        return options.keySet().containsAll(requiredOptions);
    }

    protected List<OptionSpec> allowedOptions;
    protected Set<OptionSpec> requiredOptions;
    // currently support one value per option, change?
    protected Map<OptionSpec,MeasuringOption> options;

    private List<ApplicationState> observers;

    private BenchmarkResult measurement;

    protected void putMeasurement(String key, String value) {
        measurement.put(key, value);
    }

    public BenchmarkResult getMeasurement() {
        if (this.options != null) {
            if (!hasOptions) {
                for (MeasuringOption op : options.values()) {
                    this.measurement.put(
                        op.toStringPair().first,
                        op.toStringPair().second);
                }
                hasOptions = true;
            }
        }
        // if (this.benchmark instanceof MeasuringTool) {
        //     this.measurement.putAll(((MeasuringTool)this.benchmark).getMeasurement());
        // }
        return this.measurement;
    }

    public void clearMeasurement() {
        this.measurement = new BenchmarkResult();
    }

    private static final List<String> emptyList = new ArrayList<String> ();
    public List<String> getFilenames() {
        return emptyList;
    }

    public int getRounds() {
        return rounds;
    }

    private boolean hasOptions;
    private Benchmark benchmark;
    private int rounds;

    public static class UnsupportedOptionException extends RuntimeException {}
}

