package fi.helsinki.cs.tituomin.nativebenchmark;

public class MockObject {

    public boolean booleanField;
    public byte byteField;
    public char charField;
    public double doubleField;
    public float floatField;
    public int intField;
    public long longField;
    public short shortField;
    public Object objectField;

    public static boolean booleanStaticField;
    public static byte byteStaticField;
    public static char charStaticField;
    public static double doubleStaticField;
    public static float floatStaticField;
    public static int intStaticField;
    public static long longStaticField;
    public static short shortStaticField;
    public static Object objectStaticField;

    public MockObject() {
         booleanField = true;
         byteField = 1;
         charField = 2;
         doubleField = 1.2;
         floatField = 3.1;
         intField = 3;
         longField = 4;
         shortField = 5;
         objectField = this;

         booleanStaticField = true;
         byteStaticField = 1;
         charStaticField = 2;
         doubleStaticField = 1.2;
         floatStaticField = 3.1;
         intStaticField = 3;
         longStaticField = 4;
         shortStaticField = 5;
         objectStaticField = this;
    }
}
