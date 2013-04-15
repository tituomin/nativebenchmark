package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.benchmark.JavaCounterparts;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import android.util.Log;

// Important! Imported as an example class.
import android.content.pm.PermissionInfo;

public class BenchmarkParameter implements Iterable<Integer> {

    private native int initReturnvalues();
    private native void freeReturnvalues();

    public void setUp() {
        initReturnvalues();
        JavaCounterparts.initParams(this);
    }

    public void tearDown() {
        freeReturnvalues();
    }
    

    public static int DEFAULTSIZE = 16;
    private static int RANGE = 8;
    private static int MAXSIZE = DEFAULTSIZE * RANGE;

    public BenchmarkParameter() {
        index = 1;
        if (!generated) {
            generateAll();
            generated = true;
        }

        for (int i = 0; i < RANGE + 1; i++) {
            if (STRINGS[i] == null) {
                Log.v("Parameter", "STRINGS is null at " + i);
            }
            if (OBJECTS[i] == null) {
                Log.v("Parameter", "OBJECTS is null at " + i);
            }
            if (THROWABLES[i] == null) {
                Log.v("Parameter", "THROWABLES is null at " + i);
            }
            if (BOOLEAN_ARRAYS[i] == null) {
                Log.v("Parameter", "BOOLEAN_ARRAYS is null at " + i);
            }
            if (BYTE_ARRAYS[i] == null) {
                Log.v("Parameter", "BYTE_ARRAYS is null at " + i);
            }
            if (CHAR_ARRAYS[i] == null) {
                Log.v("Parameter", "CHAR_ARRAYS is null at " + i);
            }
            if (DOUBLE_ARRAYS[i] == null) {
                Log.v("Parameter", "DOUBLE_ARRAYS is null at " + i);
            }
            if (FLOAT_ARRAYS[i] == null) {
                Log.v("Parameter", "FLOAT_ARRAYS is null at " + i);
            }
            if (INT_ARRAYS[i] == null) {
                Log.v("Parameter", "INT_ARRAYS is null at " + i);
            }
            if (LONG_ARRAYS[i] == null) {
                Log.v("Parameter", "LONG_ARRAYS is null at " + i);
            }
            if (SHORT_ARRAYS[i] == null) {
                Log.v("Parameter", "SHORT_ARRAYS is null at " + i);
            }
            if (OBJECT_ARRAYS[i] == null) {
                Log.v("Parameter", "OBJECT_ARRAYS is null at " + i);
            }
        }
    }


    // must call initreturnvalues and freereturnvalues after...
    public void setIndex(int index) {
        if (index < (RANGE + 1)) {
            this.index = index;
        }
        else {
            throw new IllegalArgumentException("Requested size too large. " + index);
        }
    }

    public int getIndex() {
        return this.index;
    }

    public Iterator<Integer> iterator() {
        return new RangeIterator();
    }

    private class RangeIterator implements Iterator<Integer> {
        private int index;
        public RangeIterator() {
            index = -1;
        }
        public boolean hasNext() {
            return index < RANGE;
        }
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            index++;
            setIndex(index);
            return DEFAULTSIZE * index;
            
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public boolean[] retrieveBooleanArray() {
        return BOOLEAN_ARRAYS[index];
    }
    public byte[] retrieveByteArray() {
        return BYTE_ARRAYS[index];
    }
    public char[] retrieveCharArray() {
        return CHAR_ARRAYS[index];
    }
    public double[] retrieveDoubleArray() {
        return DOUBLE_ARRAYS[index];
    }
    public float[] retrieveFloatArray() {
        return FLOAT_ARRAYS[index];
    }
    public int[] retrieveIntArray() {
        return INT_ARRAYS[index];
    }
    public long[] retrieveLongArray() {
        return LONG_ARRAYS[index];
    }
    public short[] retrieveShortArray() {
        return SHORT_ARRAYS[index];
    }
    public Object[] retrieveObjectArray() {
        return OBJECT_ARRAYS[index];
    }
    public Object retrieveObject() {
        return OBJECTS[index];
    }
    public Class retrieveClass() {
        // todo: causes bump in measurements
        return OBJECTS[index].getClass();
    }
    public String retrieveString() {
        return STRINGS[index];
    }
    public Throwable retrieveThrowable() {
        return THROWABLES[index];
    }

    // -------------------------------------------------

    private static void generateAll() {
        int index = RANGE - 1; int size = MAXSIZE - DEFAULTSIZE;
        generateMax();
        while (index > -1) {
            BOOLEAN_ARRAYS[index] = Arrays.copyOf(BOOLEAN_ARRAYS[RANGE], size);
            CHAR_ARRAYS[index] = Arrays.copyOf(CHAR_ARRAYS[RANGE], size);
            BYTE_ARRAYS[index] = Arrays.copyOf(BYTE_ARRAYS[RANGE], size);
            INT_ARRAYS[index] = Arrays.copyOf(INT_ARRAYS[RANGE], size);
            LONG_ARRAYS[index] = Arrays.copyOf(LONG_ARRAYS[RANGE], size);
            SHORT_ARRAYS[index] = Arrays.copyOf(SHORT_ARRAYS[RANGE], size);
            DOUBLE_ARRAYS[index] = Arrays.copyOf(DOUBLE_ARRAYS[RANGE], size);
            FLOAT_ARRAYS[index] = Arrays.copyOf(FLOAT_ARRAYS[RANGE], size);

            OBJECT_ARRAYS[index] = Arrays.copyOf(OBJECT_ARRAYS[RANGE], size);

            OBJECTS[index] = OBJECT;
            THROWABLES[index] = THROWABLE;
            STRINGS[index] = STRING_BUILDER.substring(0, size);

            index--;
            size -= DEFAULTSIZE;
        } 
    }

    private static void generateMax() {
        char c = 0;
        boolean b = true;
        byte by = 0;
        int v = 0;
        long l = 0;
        short s = 0;
        double d = 0;
        float f = 0;

        BOOLEAN_ARRAYS[RANGE] = new boolean[MAXSIZE];
        CHAR_ARRAYS[RANGE] = new char[MAXSIZE];
        BYTE_ARRAYS[RANGE] = new byte[MAXSIZE];
        INT_ARRAYS[RANGE] = new int[MAXSIZE];
        LONG_ARRAYS[RANGE] = new long[MAXSIZE];
        SHORT_ARRAYS[RANGE] = new short[MAXSIZE];
        DOUBLE_ARRAYS[RANGE] = new double[MAXSIZE];
        FLOAT_ARRAYS[RANGE] = new float[MAXSIZE];
        OBJECT_ARRAYS[RANGE] = new Object[MAXSIZE];

        for (int i = 0; i < MAXSIZE; i++) {
            STRING_BUILDER.append(c);

            BOOLEAN_ARRAYS[RANGE][i] = b;
            CHAR_ARRAYS[RANGE][i] = c;
            BYTE_ARRAYS[RANGE][i] = by;
            INT_ARRAYS[RANGE][i] = v;
            LONG_ARRAYS[RANGE][i] = l;
            SHORT_ARRAYS[RANGE][i] = s;
            DOUBLE_ARRAYS[RANGE][i] = d;
            FLOAT_ARRAYS[RANGE][i] = f;
            OBJECT_ARRAYS[RANGE][i] = OBJECT;
            OBJECTS[RANGE] = OBJECT;
            THROWABLES[RANGE] = THROWABLE;

            b = !b;
            by =  (byte)((by + 1) % Byte.MAX_VALUE);
            v = (v + 1) % Integer.MAX_VALUE;
            l = (l + 1) % Long.MAX_VALUE;
            s = (short)((s + 1) % Short.MAX_VALUE);
            d = (d + 0.1);
            f = (f + 0.1f);
            c = (char)((char)(c + '\u0001') % (char)Character.MAX_VALUE);
        }
        STRINGS[RANGE] = STRING_BUILDER.substring(0, MAXSIZE);
    }

    private int index;
    private static boolean generated = false;

    private static final StringBuilder STRING_BUILDER = new StringBuilder();
    private static final Object OBJECT                = new PermissionInfo();
    private static final Throwable THROWABLE          = new Exception();

    private static final String[] STRINGS             = new String[RANGE + 1];
    private static final Object[] OBJECTS             = new PermissionInfo[RANGE + 1];
    private static final Throwable[] THROWABLES       = new Exception[RANGE + 1];

    private static final boolean[][] BOOLEAN_ARRAYS   = new boolean[RANGE + 1][];
    private static final byte[][] BYTE_ARRAYS         = new byte[RANGE + 1][];
    private static final char[][] CHAR_ARRAYS         = new char[RANGE + 1][];
    private static final double[][] DOUBLE_ARRAYS     = new double[RANGE + 1][];
    private static final float[][] FLOAT_ARRAYS       = new float[RANGE + 1][];
    private static final int[][] INT_ARRAYS           = new int[RANGE + 1][];
    private static final long[][] LONG_ARRAYS         = new long[RANGE + 1][];
    private static final short[][] SHORT_ARRAYS       = new short[RANGE + 1][];
    private static final Object[][] OBJECT_ARRAYS     = new Object[RANGE + 1][];

}

