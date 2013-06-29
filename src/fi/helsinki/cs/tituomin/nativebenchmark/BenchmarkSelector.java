package fi.helsinki.cs.tituomin.nativebenchmark;


//import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkInitialiser;
//import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
//import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRunner;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.LinuxPerfRecordTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

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
        stateChanger = new StateChanger();

        configurations = initConfig();
        if (configurations != null) {
            initSpinner(configurations);
            File sd = Environment.getExternalStorageDirectory();
            dataDir = new File(sd, "results");
            dataDir.mkdir();

            // pre-enlarges the heap
            this.allocationArray = new byte[1024 * 1024 * 100];
        }

    }

    private Map<String,ToolConfig> initConfig() {
        File configFile = new File(Environment.getExternalStorageDirectory(), "nativebenchmark_setup.json");
        String jsonContents = null;

        try {
            if (!configFile.exists()) {
                InputStream templateStream = getResources().openRawResource(
                    R.raw.setup);
                OutputStream configFileStream = Utils.makeOutputStream(
                    configFile, false);
                Utils.copyStream(templateStream, configFileStream);
            }
            jsonContents = Utils.readFileToString(configFile);
            return ToolConfig.readConfigurations(jsonContents);
        }
        catch (Exception e) {
            String msg = getResources().getString(R.string.config_error);
            updateState(ApplicationState.State.INIT_FAIL, msg);
            Log.e(TAG, msg, e);
            stateChanger.run();
            return null;
        }
    }
    
    private void initSpinner(Map<String,ToolConfig> conf) {
        Spinner spinner = (Spinner) findViewById(R.id.config_spinner);
        String keys[] = conf.keySet().toArray(new String[1]);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, keys);

        int indexOfDefault = -1;
        while (++indexOfDefault < keys.length && 
               !keys[indexOfDefault].equals("default"));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(
                    AdapterView<?> parent, View view,
                    int pos, long id) {
                    // An item was selected. You can retrieve the selected item using
                    // parent.getItemAtPosition(pos)
                    selectedConfiguration = (String) parent.getItemAtPosition(pos);
                }

                public void onNothingSelected(AdapterView<?> parent) {
                    // Another interface callback
                
                }});

        spinner.setSelection(indexOfDefault);

    }

    public void displayMessage(ApplicationState.State state, String messa)  {
        displayMessage(state.stringId, message);
    }

    public void displayMessage(int id) {
        this.resultView.setText(id);
    }

    public void displayMessage(String message) {
        this.resultView.setText(message);
    }

    public void displayMessage(int id, String message) {
        this.resultView.setText(getResources().getString(id) + " " + message);
    }

    public void updateState(ApplicationState.State state) {
        updateState(state, null);
    }
    public void updateState(ApplicationState.State state, String message) {
        synchronized(this) {
            this.state = state;
            this.message = message;
        }
    }

    private class StateChanger implements Runnable {
        public void run() {
            synchronized(BenchmarkSelector.this) {
                if (message == null) {
                    displayMessage(state.stringId);
                }
                else {
                    displayMessage(state.stringId, message);
                }

                switch (state) {
                case MEASURING_STARTED:
                    wakeLock.acquire();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    expPick.setEnabled(false);
                    numPick.setEnabled(false);
                    switchButton(button);
                    LogAccess.start();
                    state = ApplicationState.State.MILESTONE;
                    break;
                case MILESTONE:
                    break;

                case ERROR:
                case INTERRUPTED:
                    // intended fallthrough
                case MEASURING_FINISHED:
                    LogAccess.end();
                    stateThread.interrupt();
                    wakeLock.release();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    try {
                        LogAccess.dumpLog(dataDir);
                    }
                    catch (IOException e) {
                        displayMessage(ApplicationState.State.ERROR, "Could not save log file.");
                    }
                    // intended fallthrough
                    notifyFinished();
                case INITIALISED:
                    resetButton(button);
                    numPick.setEnabled(true);
                    expPick.setEnabled(true);
                    break;
                case INIT_FAIL:
                }
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
                                displayMessage(ApplicationState.State.INTERRUPTING.stringId);
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
        final Resources resources = getResources();

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
                    
                    runner.runBenchmarks(BenchmarkSelector.this, configurations.get(selectedConfiguration), dataDir);
                }});

        stateThread = new Thread(
            new Runnable () {
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        runOnUiThread(stateChanger);
                        try {
                            Thread.sleep(5000);
                        }
                        catch (InterruptedException e) {
                            break;
                        }}}});

        this.updateState(ApplicationState.State.MEASURING_STARTED);
        stateThread.start();
        measuringThread.start();
    }

    private void notifyFinished() {
        Uri alarmSound = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_NOTIFICATION);

        Notification.Builder mBuilder =
            new Notification.Builder(this)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getResources().getText(R.string.measuring_finished))
            .setContentText(getResources().getText(R.string.measuring_finished))
            .setSound(alarmSound);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, BenchmarkSelector.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(BenchmarkSelector.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
            stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
                                          );
        //        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(1, mBuilder.build());
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
    private Thread stateThread;
    private byte[] allocationArray;
    private StateChanger stateChanger;
    private File dataDir;

    private ApplicationState.State state;
    private String message;
    private String selectedConfiguration;

    private static final String TAG = "BenchmarkSelector";

    private Map<String,ToolConfig> configurations;

    static {
        System.loadLibrary("nativebenchmark");
    }

}
