package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.PowerManager;
import android.util.Log;
import fi.helsinki.cs.tituomin.nativebenchmark.L;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class BenchmarkController implements ApplicationState {

    // todo: state changing things synchronized

    public BenchmarkController(Context aContext, File dataDir) {
        this.detailedState = new ApplicationState.DetailedState(this);
        this.dataDir = dataDir;
        this.listeners = new Listeners();

        PowerManager pm = (PowerManager) aContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Benchmarking");
        ActivityManager am = (ActivityManager) aContext.getSystemService(Context.ACTIVITY_SERVICE);
        this.resources = aContext.getResources();
        int memoryClass = am.getLargeMemoryClass();
        Log.v("Selector", "Memory size " + Runtime.getRuntime().maxMemory());
        Log.v("onCreate", "memoryClass:" + Integer.toString(memoryClass));

    }

    public Resources getResources() {
        return this.resources;
    }

    public void updateState(ApplicationState.State state) {
        updateState(state, null);
    }
    public void updateState(ApplicationState.State state, String message) {
        synchronized(this) {
            this.detailedState.state = state;
            this.detailedState.message = message;

            switch (state) {
            case MEASURING_STARTED:
                try {
                    LogAccess.start(dataDir);
                }
                catch (IOException e) {
                    this.detailedState.state = ApplicationState.State.ERROR;
                    this.detailedState.message = "Could not initialize log file.";
                    Log.e(TAG, this.detailedState.message);
                }
                wakeLock.acquire();
                break;
            case ERROR:
            case INTERRUPTED:
            case MEASURING_FINISHED:
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
                LogAccess.end();
            case INITIALISED:
            case INIT_FAIL:
            case MILESTONE:
                if (this.listeners.milestoneListener != null) {
                    this.listeners.milestoneListener.stateUpdated(this.detailedState);
                }
            }
        }
    }
    public ApplicationState.DetailedState getState() {
        synchronized(this) {
            return new ApplicationState.DetailedState(this, this.detailedState);
        }
    }
    public boolean userWantsToRetry(Exception e) {
        return false; // TODO: make interaction configurable
    }

    private class BenchRunnable implements Runnable{
        BenchRunnable(BenchmarkRunner runner, ToolConfig configuration) {
            this.runner = runner;
            this.configuration = configuration;
        }
        public void run() {
            this.runner.runBenchmarks(
                BenchmarkController.this,
                this.configuration,
                dataDir);
        }
        private BenchmarkRunner runner;
        private ToolConfig configuration;
    }

    public void startMeasuring(BenchmarkRunner runner, ToolConfig configuration) {
        BenchmarkRunner.BenchmarkSet set = configuration.getBenchmarkSet();
        runner.setBenchmarkSet(set);
        measuringThread = new Thread(new BenchRunnable(runner, configuration));
        this.updateState(ApplicationState.State.MEASURING_STARTED);
        measuringThread.start();

        // Settings parameter types saved for posterity.
        // ---------------------------------------------
        //
        // CharSequence appCheksum = resources.getText(R.string.app_checksum);
        // CharSequence appRevision = resources.getText(R.string.app_revision);
        // File cacheDir = this.getCacheDir();
        // long repetitions = this.repetitions;
        // long allocatingRepetitions = Long.parseLong(textValue(R.id.alloc_reps));
        // String benchmarkSubstring = textValue(R.id.benchmark_substring).toLowerCase();
        // boolean runAllBenchmarks = isChecked(R.id.checkbox_long);
        // boolean runAtMaxSpeed = isChecked(R.id.checkbox_max);
        // BenchmarkRunner.BenchmarkSet benchmarkSet =
        //     isChecked(R.id.run_alloc) ?
        //     BenchmarkRunner.BenchmarkSet.ALLOC :
        //     BenchmarkRunner.BenchmarkSet.NON_ALLOC;
    }

    public void interruptMeasuring() {
        MeasuringTool.userInterrupt();
        measuringThread.interrupt();
    }

    public void addListener(ApplicationStateListener listener, ApplicationState.State state) {
        if (state == ApplicationState.State.MILESTONE) {
            this.listeners.milestoneListener = listener;
        }
    }

    public void removeListeners() {
        this.listeners = null;
    }

    private class Listeners {
        public ApplicationStateListener milestoneListener;
        public Listeners() {
            milestoneListener = null;
        }
    }

    private Listeners listeners;
    private ApplicationState.State state;
    private String message;
    private ApplicationState.DetailedState detailedState;
    private PowerManager.WakeLock wakeLock;
    private File dataDir;
    private Thread measuringThread;
    private static final String TAG = "BenchmarkController";
    private Resources resources;

}
