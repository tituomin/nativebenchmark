package fi.helsinki.cs.tituomin.nativebenchmark;
import android.os.PowerManager;
import java.util.LinkedList;
import java.util.List;
import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;

public class BenchmarkController implements ApplicationState {

    // todo: state changing things synchronized

    public BenchmarkController() {
        this.listeners = new LinkedList<ApplicationState>();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Benchmarking");
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getLargeMemoryClass();
        Log.v("Selector", "Memory size " + Runtime.getRuntime().maxMemory());
        Log.v("onCreate", "memoryClass:" + Integer.toString(memoryClass));

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
                LogAccess.start();
                wakeLock.acquire();
                break;
            case ERROR:
            case INTERRUPTED:
            case MEASURING_FINISHED:
                LogAccess.end();
                wakeLock.release();
                try {
                    LogAccess.dumpLog(dataDir);
                }
                catch (IOException e) {
                    Log.v(); //TODO
                    //TODO
                    //displayMessage(ApplicationState.State.ERROR, "Could not save log file.");
                }
            case INITIALISED:
            case INIT_FAIL:
            case MILESTONE:
            }
        }
    }
    public ApplicationState.DetailedState getState() {
        synchronized(this) {
            return new ApplicationState.DetailedState(this.detailedState):
        }
    }

    public void startMeasuring(BenchmarkRunner runner) {
        allocationArray = null;
        measuringThread = new Thread(
            new Runnable () {
                public void run() {
                    runner.runBenchmarks(BenchmarkContoller.this, configurations.get(selectedConfiguration), dataDir);
                }});
        this.updateState(ApplicationState.State.MEASURING_STARTED);
        measuringThread.start();

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

    public void addListener(ApplicationState listener) {
        this.listeners.add(listener);
    }

    public void removeListeners() {
        this.listeners = null;
    }

    private List<ApplicationState> listeners;
    private ApplicationState.State state;
    private String message;
    private ApplicationState.DetailedState detailedState;
}
