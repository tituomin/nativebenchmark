package fi.helsinki.cs.tituomin.nativebenchmark;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;


import android.util.Log;


public class BenchmarkResult {

    public BenchmarkResult() {
        values = new String[ESTIMATED_CAPACITY];
        valueCount = 0;
    }

    public void put(String label, String value) {
        Integer labelIndex = labelIndexes.get(label);
        if (labelIndex == null) {
            lastIndex++;
            if (lastIndex > labels.length) {
                Log.e("BenchmarkResults", "Error, too many kinds of values, increase capacity!");
            }
            labels[lastIndex] = label;
            labelIndexes.put(label, lastIndex);
            labelIndex = lastIndex;
        }
        values[labelIndex] = value;
        valueCount++;
    }

    public String get(String label) {
        return values[labelIndexes.get(label)];
    }

    public String get(int i) {
        return values[i];
    }

    public void putAll(BenchmarkResult other) {
        String[] otherValues = other.getValues();
        for (int i = 0; i < size(); i++) {
            if (otherValues[i] != null) {
                values[i] = otherValues[i];
                valueCount++;
            }
        }
    }

    public void putAll(Map<String,String> map) {
        for (Entry<String,String> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public String[] getValues() {
        return values;
    }

    public static String getLabel(int i) {
        return labels[i];
    }

    public static String[] labels() {
        return labels;
    }

    public boolean isEmpty() {
        return (valueCount == 0);
    }

    public static int size() {
        return (lastIndex + 1);
    }

    private static final int ESTIMATED_CAPACITY = 200;
    private String[] values;
    private int valueCount;

    private static String[] labels = new String[ESTIMATED_CAPACITY];
    private static Map<String,Integer> labelIndexes = new HashMap<String,Integer> (ESTIMATED_CAPACITY);
    private static int lastIndex = -1;
}
