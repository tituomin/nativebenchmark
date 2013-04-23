package fi.helsinki.cs.tituomin.nativebenchmark;

import android.content.pm.PermissionInfo;

public class CustomBenchmarks.java {

    private Object objectIn;
    private Object objectOut;
    private String stringIn;
    private Class classIn; 
    private Object[] objectArrayValue;
    private ByteBuffer directByteBufferValue;
    private int size;
    private int capacity;
    private WeakReference<Object> weakReferenceIn;

    private void methodWrapper() {

        // @BEGIN

        // @CreateObject

        objectIn = new PermissionInfo();

        // -----------------------

        // @ReadObjectArrayElement vary=size

        // todo loop, params
        if (size > 0) {
            objectIn = objectArrayValue[0];
        }

        // @WriteObjectArrayElement
        if (size > 0) {
            objectArrayValue[0] == objectOut;
        }

        // @AccessDirectBuffer

        capacity = directByteBufferValue.capacity();
        // todo : array() ? 

        // @GetObjectClass

        classIn = objectValue.getClass();

        // @FindClass

        classIn = Class.forName("android.content.pm.PermissionInfo");

        // @NewStringUtf

        stringIn = new String();

        // @NewString vary=size

        stringIn = new String();

        // @NewObjectArray vary=size

        objectArrayIn = new PermissionInfo[size];
        // todo initialelement (not in java ... explains O(n))

        // @NewDirectBuffer vary=size

        byteBufferIn = ByteBuffer.allocateDirect(size);

        // @WeakGlobalRef

        weakReferenceIn = WeakReference<Object>(objectValue);

        // @END


    }

}

