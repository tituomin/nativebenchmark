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

public class ToolConfig {

    public static Map<String, ToolConfig> readConfigurations(File jsonFile)
    throws JSONException {
        JSONObject cfgObject = new JSONObject(readToString(jsonFile));
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

    public static String readToString(File file) {
        return ""; // todo
    }

    public Iterator<MeasuringTool> tools() {
        return new ToolIterator();
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
            try {
                return createTool(toolArray.getJSONObject(++currentToolIndex));
            }
            catch (JSONException e) {
                Log.e("ToolConfig", "Wrong structure in JSON tool array");
                throw new NoSuchElementException();
            }
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private MeasuringTool createTool(JSONObject specs) throws JSONException, ClassNotFoundException, NoSuchMethodException, InstantiationException {
        Class<?> _class = Class.forName(TOOL_PACKAGE + "." + specs.getString("class"));
        Constructor<?> ctor = _class.getConstructor(Integer.class, Long.class, Long.class);

        long repetitions;
        int rounds;

        // todo
        long defaultRepetitions = 0;
        int defaultRounds = 0;
        long allocrepetitions = 0;

        repetitions = specs.optLong("repetitions", contents.optLong(specs.optString("repetitions"), defaultRepetitions));
        rounds = specs.optInt("rounds", contents.optInt(specs.optString("rounds"), defaultRounds));

        // todo allocarepetitions
        Object tool = ctor.newInstance(rounds, repetitions, allocrepetitions);
        return (MeasuringTool) tool;
    }

    private JSONObject contents;
    private static final String TOOL_PACKAGE = "fi.helsinki.cs.tituomin.nativebenchmark";

}
