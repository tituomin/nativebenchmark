package fi.helsinki.cs.tituomin.nativebenchmark;

public class MockObject {

    public boolean jbooleanField;
    public byte jbyteField;
    public char jcharField;
    public double jdoubleField;
    public float jfloatField;
    public int jintField;
    public long jlongField;
    public short jshortField;
    public Object jobjectField;

    public static boolean jbooleanStaticField;
    public static byte jbyteStaticField;
    public static char jcharStaticField;
    public static double jdoubleStaticField;
    public static float jfloatStaticField;
    public static int jintStaticField;
    public static long jlongStaticField;
    public static short jshortStaticField;
    public static Object jobjectStaticField;

    public MockObject() {
         jbooleanField = true;
         jbyteField = 1;
         jcharField = 2;
         jdoubleField = 1.2;
         jfloatField = 3.1f;
         jintField = 3;
         jlongField = 4;
         jshortField = 5;
         jobjectField = this;

         jbooleanStaticField = true;
         jbyteStaticField = 1;
         jcharStaticField = 2;
         jdoubleStaticField = 1.2;
         jfloatStaticField = 3.1f;
         jintStaticField = 3;
         jlongStaticField = 4;
         jshortStaticField = 5;
         jobjectStaticField = this;
    }
}
