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
    MockObject mockObject = benchmarkParameter.retrieveMockObject();
    String stringIn = benchmarkParameter.retrieveString();
    Object[] objectArrayValue = benchmarkParameter.retrieveObjectArray();
    ByteBuffer directByteBufferValue = benchmarkParameter.retrieveDirectByteBuffer();
    int current_size = benchmarkParameter.getSize();
    int __a = 2, __b = 3;
    int localPersistentValue = 0;
    WeakReference<Object> weakReferenceIn;
    // @inits-end

    private void methodWrapper() {


        // @CreateObject direction=jj alloc=true

        objectIn = new PermissionInfo();

        // @ReadObjectArrayElement direction=jj

        // todo loop, params
        if (current_size > 0) {
            objectIn = objectArrayValue[0];
        }

        // @WriteObjectArrayElement direction=jj
        if (current_size > 0) {
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

        // @NewStringUtf direction=jj alloc=true

        stringIn = new String();

        // @NewString vary=size direction=jj alloc=true

        stringIn = new String();

        // @NewObjectArray vary=size direction=jj alloc=true

        objectArrayIn = new PermissionInfo[current_size];
        // todo initialelement (not in java ... explains O(n))

        // @NewDirectBuffer vary=size direction=jj alloc=true

        byteBufferIn = ByteBuffer.allocateDirect(current_size);

        // @WeakGlobalRef direction=jj

        weakReferenceIn = new WeakReference(objectValue);
        weakReferenceIn = null;

        // @END


    }

}

