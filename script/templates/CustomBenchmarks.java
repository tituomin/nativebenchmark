package fi.helsinki.cs.tituomin.nativebenchmark.benchmark;

import android.content.pm.PermissionInfo;
import java.nio.ByteBuffer;


public class CustomBenchmarks {

    // @BEGIN


    // @inits
    Object objectIn;
    Object[] objectArrayIn;
    Class classIn;
    int capacityIn;
    ByteBuffer byteBufferIn;
    Object objectValue = benchmarkParameter.retrieveObject();
    String stringIn = benchmarkParameter.retrieveString();
    Object[] objectArrayValue = benchmarkParameter.retrieveObjectArray();
    ByteBuffer directByteBufferValue = benchmarkParameter.retrieveDirectByteBuffer();
    int size = benchmarkParameter.getSize();
    int __a = 2, __b = 3;
    WeakReference<Object> weakReferenceIn;
    // @inits-end

    private void methodWrapper() {


        // @CreateObject direction=jj

        objectIn = new PermissionInfo();

        // @ReadObjectArrayElement vary=size direction=jj

        // todo loop, params
        if (size > 0) {
            objectIn = objectArrayValue[0];
        }

        // @WriteObjectArrayElement direction=jj
        if (size > 0) {
            objectArrayValue[0] = objectValue;
        }

        // @AccessDirectBuffer direction=jj

        capacityIn = directByteBufferValue.capacity();
        // todo : array() ? 

        // @GetObjectClass direction=jj

        classIn = objectValue.getClass();

        // @FindClass direction=jj

        try {
            classIn = Class.forName("android.content.pm.PermissionInfo");
        }
        catch (ClassNotFoundException e) {
            
        }

        // @NewStringUtf direction=jj

        stringIn = new String();

        // @NewString vary=size direction=jj

        stringIn = new String();

        // @NewObjectArray vary=size direction=jj

        objectArrayIn = new PermissionInfo[size];
        // todo initialelement (not in java ... explains O(n))

        // @NewDirectBuffer vary=size direction=jj

        byteBufferIn = ByteBuffer.allocateDirect(size);

        // @WeakGlobalRef direction=jj

        weakReferenceIn = new WeakReference(objectValue);

        // @END


    }

}

