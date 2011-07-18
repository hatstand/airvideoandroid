LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE  := videokit
#FFMPEG_LIBS := $(shell find ffmpeg -name '*.a')
FFMPEG_LIBS := ffmpeg/libavdevice/libavdevice.a \
ffmpeg/libavformat/libavformat.a \
ffmpeg/libavcodec/libavcodec.a \
ffmpeg/libavfilter/libavfilter.a \
ffmpeg/libswscale/libswscale.a \
ffmpeg/libavutil/libavutil.a \
ffmpeg/libpostproc/libpostproc.a
LOCAL_CFLAGS += -Iffmpeg -Ivideokit --std=c99
LOCAL_LDLIBS += -llog -ljnigraphics $(FFMPEG_LIBS)
LOCAL_SRC_FILES := videokit/jni_interface.c 
include $(BUILD_SHARED_LIBRARY)

