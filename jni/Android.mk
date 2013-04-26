
LOCAL_PATH := $(abspath $(call my-dir))

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm # this did it
LOCAL_MODULE    := nativebenchmark
LOCAL_SRC_FILES := nativebenchmark.c nativerunners.c returnvalues.c custom_benchmarks.c

LOCAL_LDLIBS := -Wl,--build-id -llog 

include $(BUILD_SHARED_LIBRARY)
