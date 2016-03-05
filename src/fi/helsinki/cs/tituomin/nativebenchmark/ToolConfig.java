package fi.helsinki.cs.tituomin.nativebenchmark;

import android.os.Environment;
import android.util.Log;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ToolConfig implements Iterable<MeasuringTool> {

    public static Map<String, ToolConfig> readConfigurations(String jsonConfig)
    throws JSONException {
        JSONObject cfgObject = new JSONObject(jsonConfig);
        Map<String, ToolConfig> result = new HashMap<String, ToolConfig> ();
        Iterator i = cfgObject.keys();
        while (i.hasNext()) {
            String key = (String) i.next();
            result.put(key, new ToolConfig(cfgObject.getJSONObject(key)));
        }
        return result;
    }

    public static Map<String,ToolConfig> readConfigFile() throws IOException, JSONException {
        String jsonContents = null;
        File configFile = new File(
            Environment.getExternalStorageDirectory(),
            "nativebenchmark_setup.json"
        );
        jsonContents = Utils.readFileToString(configFile);
        return ToolConfig.readConfigurations(jsonContents);
    }

    public ToolConfig(JSONObject job) {
        this.globalOptions = job;
    }

    public String toString() {
        return this.globalOptions.toString();
    }

    public Iterator<MeasuringTool> iterator() {
        try {
            return new ToolIterator();
        }
        catch (JSONException e) {
            Log.e("ToolConfig", "Error reading json config", e);
        }
        return null;
    }

    private class ToolIterator implements Iterator<MeasuringTool> {
        private JSONArray toolArray;
        private int currentToolIndex;

        public ToolIterator () throws JSONException {
            currentToolIndex = -1;
            toolArray = globalOptions.getJSONArray("tools");
        }
        public boolean hasNext() {
            return currentToolIndex + 1 < toolArray.length();
        }
        public MeasuringTool next() {
            MeasuringTool tool = null;
            try {
                tool = createTool(toolArray.getJSONObject(++currentToolIndex));
                if (tool == null) {
                    throw new NoSuchElementException();
                }
            }
            catch (JSONException e) {
                Log.e("ToolConfig", "Error reading json config", e);
            }
            return tool;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private MeasuringTool createTool(JSONObject toolOptions) {
        
        MeasuringTool tool = null;
        try {
            long repetitions = toolOptions.optLong(
                "repetitions", globalOptions.optLong(
                    toolOptions.optString("repetitions"), this.defaultRepetitions));

            // todo
            int defaultRounds = 1;
            // todo

            int rounds = toolOptions.optInt(
                "rounds", globalOptions.optInt(
                    toolOptions.optString("rounds"), defaultRounds));

            long allocRepetitions = toolOptions.optLong(
                "alloc_repetitions", globalOptions.optLong(
                    toolOptions.optString("alloc_rounds"), this.defaultAllocRepetitions));

            boolean warmup = toolOptions.optBoolean("warmup", false);

            boolean runAllBenchmarks = toolOptions.optBoolean("run_all", this.defaultRunAllBenchmarks);

            Class<?> _class = Class.forName(TOOL_PACKAGE + "." + toolOptions.getString("class"));

            Constructor<?> ctor = _class.getConstructor(
                Integer.TYPE, Long.TYPE, Long.TYPE, Boolean.TYPE, Boolean.TYPE);

            Log.v("ToolConfig", "Tool instantiation " + rounds + " " + repetitions + " " + warmup);

            try {
                tool = (MeasuringTool)ctor.newInstance(
                    rounds, repetitions, allocRepetitions,
                    warmup, runAllBenchmarks);
            }
            catch (InvocationTargetException e) {
                Log.e("ToolConfig", "Constructor exception", e.getCause());
            }
            tool.setDescription(toolOptions.optString("description", ""));
            tool.setFilter(toolOptions.optString("filter", ""));
            tool.setExplicitGC(toolOptions.optBoolean("gc", !warmup));

            JSONObject options = toolOptions.optJSONObject ("options");
            if (options != null) {
                Iterator keys = options.keys();
                while (keys.hasNext()) {
                    String key = (String)keys.next();
                    tool.set(key, options.getString(key));
                }
            }

            
        }
        catch (Exception e) {
            Log.e("ToolConfig", "Error instantiating tool", e);
            return null;
        }
        return tool;
    }

    public ToolConfig setDefaultRepetitions(long r) {
        defaultRepetitions = r;
        return this;
    }
    public ToolConfig setDefaultAllocRepetitions(long r) {
        defaultAllocRepetitions = r;
        return this;
    }
    public ToolConfig setDefaultRunAllBenchmarks(boolean r) {
        defaultRunAllBenchmarks = r;
        return this;
    }

    private long defaultRepetitions;
    private long defaultAllocRepetitions;
    private boolean defaultRunAllBenchmarks;

    private JSONObject globalOptions;
    private static final String TOOL_PACKAGE = "fi.helsinki.cs.tituomin.nativebenchmark.measuringtool";
    private static final String TAG = "nativebenchmark.ToolConfig";

}
