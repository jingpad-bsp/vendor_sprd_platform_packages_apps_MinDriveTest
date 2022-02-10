LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)

#LOCAL_SDK_VERSION := current
LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_STATIC_JAVA_LIBRARIES := \
    vendor.sprd.hardware.radio.lite-V1.0-java

LOCAL_DEX_PREOPT := false
LOCAL_PACKAGE_NAME := MinDriveTest
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)

include $(call all-makefiles-under, $(LOCAL_PATH))
