package fi.helsinki.cs.tituomin.nativebenchmark;

public class BenchmarkParameter {

    private static int DEFAULTSIZE = 20;
    private static int MAXSIZE = 1024;

    public BenchmarkParameter() {
        size = DEFAULTSIZE;
        if (!generated) {
            generateString(); // chararray generated too
            generateBooleanArray();
            generateByteArray();
            generateIntArray();
            generateLongArray();
            generateShortArray();
            generateDoubleArray();
            generateFloatArray();
            generateObjectArray();
            generated = true;
        }
    }

    public void setSize(int size) {
        if (size <= MAXSIZE) {
            this.size = size;
        }
        else {
            throw new IllegalArgumentException("Requested size too large.");
        }
    }

    public boolean[] retrieveBooleanArray() {
        return Arrays.copyOfRange(BOOLEAN_ARRAY, 0, size);
    }
    public byte[] retrieveByteArray() {
        return Arrays.copyOfRange(BYTE_ARRAY, 0, size);
    }
    public char[] retrieveCharArray() {
        return Arrays.copyOfRange(CHAR_ARRAY, 0, size);
    }
    public double[] retrieveDoubleArray() {
        return Arrays.copyOfRange(DOUBLE_ARRAY, 0, size);
    }
    public float[] retrieveFloatArray() {
        return Arrays.copyOfRange(FLOAT_ARRAY, 0, size);
    }
    public int[] retrieveIntArray() {
        return Arrays.copyOfRange(INT_ARRAY, 0, size);
    }
    public long[] retrieveLongArray() {
        return Arrays.copyOfRange(LONG_ARRAY, 0, size);
    }
    public short[] retrieveShortArray() {
        return Arrays.copyOfRange(SHORT_ARRAY, 0, size);
    }
    public Object[] retrieveObjectArray() {
        return Arrays.copyOfRange(OBJECT_ARRAY, 0, size);
    }
    public Object retrieveObject() {
        return OBJECT;
    }
    public Class retrieveClass() {
        return CLASS;
    }
    public String retrieveString() {
        return STRING.substring(0, size);
    }
    public Throwable retrieveThrowable() {
        return THROWABLE;
    }

    // -------------------------------------------------

    private static void generateString() {
        char c = 0;
        for (int i = 0; i < MAXSIZE; i++) {
            STRING.append(c);
            CHAR_ARRAY[i] = c;
            c = (char)((char)(c + '\u0001') % (char)Character.MAX_VALUE);
        }
    }
    private static void generateBooleanArray() {
        boolean b = true;
        for (int i = 0; i < MAXSIZE; i++) {
            BOOLEAN_ARRAY[i] = b;
            b = !b;
        }
    }
    private static void generateByteArray() {
        byte b = 0;
        for (int i = 0; i < MAXSIZE; i++) {
            BYTE_ARRAY[i] = b;
            b =  (byte)((b + 1) % Byte.MAX_VALUE);
        }
    }
    private static void generateIntArray() {
        int v = 0;
        for (int i = 0; i < MAXSIZE; i++) {
            INT_ARRAY[i] = v;
            v = (v + 1) % Integer.MAX_VALUE;
        }
    }
    private static void generateLongArray() {
        long l = 0;
        for (int i = 0; i < MAXSIZE; i++) {
            LONG_ARRAY[i] = l;
            l = (l + 1) % Long.MAX_VALUE;
        }
    }
    private static void generateShortArray() {
        short s = 0;
        for (int i = 0; i < MAXSIZE; i++) {
            SHORT_ARRAY[i] = s;
            s = (short)((s + 1) % Short.MAX_VALUE);
        }
    }
    private static void generateDoubleArray() {
        double d = 0;
        for (int i = 0; i < MAXSIZE; i++) {
            DOUBLE_ARRAY[i] = d;
            d = (d + 0.1);
        }
    }
    private static void generateFloatArray() {
        float f = 0;
        for (int i = 0; i < MAXSIZE; i++) {
            FLOAT_ARRAY[i] = f;
            f = (f + 0.1f);
        }
    }
    private static void generateObjectArray() {
        for (int i = 0; i < MAXSIZE; i++) {
            OBJECT_ARRAY[i] = OBJECT;
        }
    }

    private int size;

    private static boolean generated = false;
    private static final StringBuilder STRING = new StringBuilder(MAXSIZE);
    private static final Object OBJECT = new Object();
    private static final Class CLASS = OBJECT.getClass();
    private static final Throwable THROWABLE = new Exception();
    private static final boolean[] BOOLEAN_ARRAY = new boolean[MAXSIZE];
    private static final byte[] BYTE_ARRAY = new byte[MAXSIZE];
    private static final char[] CHAR_ARRAY = new char[MAXSIZE];
    private static final double[] DOUBLE_ARRAY = new double[MAXSIZE];
    private static final float[] FLOAT_ARRAY = new float[MAXSIZE];
    private static final int[] INT_ARRAY = new int[MAXSIZE];
    private static final long[] LONG_ARRAY = new long[MAXSIZE];
    private static final short[] SHORT_ARRAY = new short[MAXSIZE];
    private static final Object[] OBJECT_ARRAY = new Object[MAXSIZE];

}

