package fi.helsinki.cs.tituomin.nativebenchmark;


import android.util.Log;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import java.io.File;
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

    public ToolConfig(JSONObject job) {
        this.contents = job;
    }

    public String toString() {
        return this.contents.toString();
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
            toolArray = contents.getJSONArray("tools");
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

    private MeasuringTool createTool(JSONObject specs) {
        
        // todo
        int defaultRounds = 1;
        // todo

        MeasuringTool tool = null;
        try {
            long repetitions = specs.optLong(
                "repetitions", contents.optLong(
                    specs.optString("repetitions"), defaultRepetitions));

            int rounds = specs.optInt(
                "rounds", contents.optInt(
                    specs.optString("rounds"), defaultRounds));

            boolean warmup = specs.optBoolean("warmup", false);

            Class<?> _class = Class.forName(TOOL_PACKAGE + "." + specs.getString("class"));

            Constructor<?> ctor = _class.getConstructor(
                Integer.TYPE, Long.TYPE, Long.TYPE, Boolean.TYPE);

            Log.v("ToolConfig", "Tool instantiation " + rounds + " " + repetitions + " " + warmup);

            try {
                tool = (MeasuringTool)ctor.newInstance(rounds, repetitions, allocRepetitions, warmup);
            }
            catch (InvocationTargetException e) {
                Log.e("ToolConfig", "Constructor exception", e.getCause());
            }
            tool.setDescription(specs.optString("description", ""));
            tool.setFilter(specs.optString("filter", ""));
            tool.setExplicitGC(specs.optBoolean("gc", !warmup));

            JSONObject options = specs.optJSONObject ("options");
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

    public ToolConfig setRepetitions(long r) {
        defaultRepetitions = r;
        return this;
    }
    public ToolConfig setAllocRepetitions(long r) {
        allocRepetitions = r;
        return this;
    }

    private long defaultRepetitions;
    private long allocRepetitions;

    private JSONObject contents;
    private static final String TOOL_PACKAGE = "fi.helsinki.cs.tituomin.nativebenchmark.measuringtool";

}
