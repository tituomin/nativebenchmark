package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.LinuxPerfRecordTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkInitialiser;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRunner;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import android.util.Pair;
import java.io.IOException;
import java.io.File;
import android.os.Environment;
import android.content.res.Resources;
import android.widget.NumberPicker;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.RadioButton;
import android.view.WindowManager;
import android.os.PowerManager;
import android.content.Context;
import android.content.DialogInterface;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActivityManager;

public class BenchmarkSelector extends Activity implements ApplicationState {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        this.retry = false;

        this.resultView = (TextView)     findViewById(R.id.resultview);
        this.button     = (Button)       findViewById(R.id.mybutton);
        this.repView    = (TextView)     findViewById(R.id.repetitions);
        this.numPick    = (NumberPicker) findViewById(R.id.picker_num);
        this.expPick    = (NumberPicker) findViewById(R.id.picker_exp);

        NumberPicker.OnValueChangeListener listener = new RepsListener();

        numPick.setMinValue(1); numPick.setMaxValue(9); numPick.setValue(1);
        expPick.setMinValue(0); expPick.setMaxValue(9); expPick.setValue(6);
        numPick.setOnValueChangedListener(listener);
        expPick.setOnValueChangedListener(listener);

        listener.onValueChange(numPick, 0, 0);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Benchmarking");

        if (getResources().getString(R.string.app_dirty).equals("1")) {
            this.resultView.setText(R.string.warning_changed);
        }

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getLargeMemoryClass();
        Log.v("Selector", "Memory size " + Runtime.getRuntime().maxMemory());
        Log.v("onCreate", "memoryClass:" + Integer.toString(memoryClass));

        // pre-enlarges the heap
        this.allocationArray = new byte[1024 * 1024 * 100];
    }

    public void setMessage(int id) {
        this.resultView.setText(id);
    }

    public void setMessage(String message) {
        this.resultView.setText(message);
    }

    public void setMessage(int id, String message) {
        this.resultView.setText(getResources().getString(id) + " " + message);
    }

    public void updateState(ApplicationState.State state) {
        runOnUiThread(new StateChanger(state));
    }
    public void updateState(ApplicationState.State state, String message) {
        runOnUiThread(new StateChanger(state, message));
    }

    private class StateChanger implements Runnable {
        private ApplicationState.State state;
        private String message;

        public StateChanger(ApplicationState.State state) {
            this.state = state;
        }
        public StateChanger(ApplicationState.State state, String message) {
            this.state = state;
            this.message = message;
        }

        public void run() {
            if (message == null) {
                setMessage(state.stringId);
            }
            else {
                setMessage(state.stringId, message);
            }

            switch (state) {
            case MEASURING:
                wakeLock.acquire();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                expPick.setEnabled(false);
                numPick.setEnabled(false);
                switchButton(button);
                break;
            case MILESTONE:
                break;

                // -----------------------------

            case ERROR:
            case INTERRUPTED:
                // intended fallthrough
            case MEASURING_FINISHED:
                wakeLock.release();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                // intended fallthrough
            case INITIALISED:
                resetButton(button);
                numPick.setEnabled(true);
                expPick.setEnabled(true);
                break;
            }
        }
    }

    public boolean userWantsToRetry(final Exception e) {
        runOnUiThread(new Runnable() {
                public void run() {
                    DialogFragment dialog = new RetryDialog(e.getMessage());
                    dialog.show(getFragmentManager(), "foo");
                }
            });
        boolean waiting = true;
        while (waiting) {
            synchronized (this) {
                try {
                    this.wait();
                    waiting = false;
                }
                catch (InterruptedException ie) {
                    waiting = true;
                }
            }
        }
        return this.retry;
    }

    private void setRetry(boolean answer) {
        this.retry = answer;
        synchronized (this) {
            this.notify();
        }
    }

    private void resetButton(Button button) {
        button.setText(R.string.start_task);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startMeasuring(v);
                }
            });
    }

    private void switchButton(Button bt) {
        bt.setText(R.string.end_task);
        bt.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    button.setText(R.string.end_task_confirmation);
                    button.setOnClickListener(
                        new View.OnClickListener() {
                            public void onClick(View v) {
                                setMessage(ApplicationState.State.INTERRUPTING.stringId);
                                MeasuringTool.userInterrupt();
                                measuringThread.interrupt();
                            }
                        });
                }
            });
    }


    private boolean isChecked(int id) {
        return ((Checkable) findViewById(id)).isChecked();
    }

    private String textValue(int id) {
        return ((TextView) findViewById(id)).getText().toString();
    }

    public void startMeasuring(View view) {
        allocationArray = null;
        Resources resources = getResources();
        measuringThread = new Thread(
            new Runnable () {
                public void run() {
                    BenchmarkRunner runner = BenchmarkRunner.INSTANCE 
                        .setAppChecksum           (resources.getText(R.string.app_checksum))
                        .setAppRevision           (resources.getText(R.string.app_revision))
                        .setCacheDir              (getCacheDir())
                        .setRepetitions           (repetitions)
                        .setAllocatingRepetitions (Long.parseLong(textValue(R.id.alloc_reps)))
                        .setBenchmarkSubstring    (textValue(R.id.benchmark_substring).toLowerCase())
                        .setRunAllBenchmarks      (isChecked(R.id.checkbox_long))
                        .setRunAtMaxSpeed         (isChecked(R.id.checkbox_max))
                        .setBenchmarkSet          (isChecked(R.id.run_alloc) ?
                                                   BenchmarkRunner.BenchmarkSet.ALLOC :
                                                   BenchmarkRunner.BenchmarkSet.NON_ALLOC);
                    
                    runner.runBenchmarks(BenchmarkSelector.this);
                }
            });
        this.updateState(ApplicationState.State.MEASURING);
        measuringThread.start();
    }

    private class RepsListener implements NumberPicker.OnValueChangeListener {
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            long exp = BenchmarkSelector.this.expPick.getValue();
            long value = BenchmarkSelector.this.numPick.getValue();
            //            rounds = BenchmarkSelector.this.roundPick.getValue();
            while (exp-- > 0) { value *= 10; }
            repetitions = value;
            repView.setText("" + repetitions);
        }
    }

    private class RetryDialog extends DialogFragment {
        private String message;

        public RetryDialog(String message) {
            this.message = message;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getResources().getText(R.string.retry_question) + ":\n" + this.message)
                .setPositiveButton(R.string.retry_answer_positive, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setRetry(true);
                        }
                    })
                .setNegativeButton(R.string.retry_answer_negative, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setRetry(false);
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private long repetitions;
    private boolean retry;
    private TextView textView, resultView, repView;
    private NumberPicker numPick, expPick;
    private Button button;
    private PowerManager.WakeLock wakeLock;
    private Thread measuringThread;
    private byte[] allocationArray;

    private static final String TAG = "BenchmarkSelector";

    static {
        System.loadLibrary("nativebenchmark");
    }

}
