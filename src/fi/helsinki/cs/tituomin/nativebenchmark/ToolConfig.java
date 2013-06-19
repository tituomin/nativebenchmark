package fi.helsinki.cs.tituomin.nativebenchmark;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.lang.reflect.Constructor;
import android.util.Log;
import java.util.NoSuchElementException;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;

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
            currentToolIndex = 0;
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

        long repetitions;
        int rounds;
        boolean warmup;
        
        // todo
        int defaultRounds = 1;
        // todo

        MeasuringTool tool = null;
        try {
            repetitions = specs.optLong(
                "repetitions", contents.optLong(
                    specs.optString("repetitions"), defaultRepetitions));

            rounds = specs.optInt(
                "rounds", contents.optInt(
                    specs.optString("rounds"), defaultRounds));

            warmup = specs.optBoolean("warmup", false);

            Class<?> _class = Class.forName(TOOL_PACKAGE + "." + specs.getString("class"));

            Constructor<?> ctor = _class.getConstructor(
                Integer.TYPE, Long.TYPE, Long.TYPE, Boolean.TYPE);

            Log.v("ToolConfig", "Tool instantiation " + rounds + " " + repetitions + " " + warmup);

            tool = (MeasuringTool)ctor.newInstance(rounds, repetitions, allocRepetitions, warmup);
            tool.setDescription(specs.optString("description", ""));

            JSONObject options = specs.optJSONObject ("options");
            if (options != null) {
                // todo here
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
