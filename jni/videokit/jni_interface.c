#include "libavformat/avformat.h"
#include "libavdevice/avdevice.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"

#include <android/bitmap.h>
#include <android/log.h>
#include "jni_interface.h"

#include <stdlib.h>
#include <stdbool.h>

AVFormatContext *pFormatCtx;

bool initted = false;
bool exiting = false;

// the fuck is this exit shit doing
#define exit exit_function_not_allowed
#define LOG_ERROR(message) __android_log_write(ANDROID_LOG_ERROR, "VideoKit", message)
#define LOG_INFO(message) __android_log_write(ANDROID_LOG_INFO, "VideoKit", message)
#define EXCEPTION_CODE 256

int throwException(JNIEnv *env, const char* message)
{
  jclass newExcCls;
  (*env)->ExceptionDescribe(env);
  (*env)->ExceptionClear(env);
  newExcCls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  (*env)->ThrowNew(env, newExcCls, message);
  (*env)->DeleteLocalRef(env, newExcCls);
  return EXCEPTION_CODE;
}

void LogCallback(void* a, int b, const char* p, va_list c) {
  __android_log_vprint(ANDROID_LOG_INFO, "ffmpeg", p, c);
}

JNIEXPORT void JNICALL Java_uk_co_halfninja_videokit_Videokit_initialise(JNIEnv *env, jobject self)
{
  if (!initted) 
  {
    LOG_INFO("Initialising VideoKit");
    av_register_all();
    avdevice_register_all();
    av_log_set_callback(LogCallback);
    av_log_set_level(AV_LOG_DEBUG);
    initted = true;
  }
  else
  {
    LOG_INFO("Already initialised Videokit, ignoring init()");
  }
}

void update(JNIEnv* env, jobject self) {
  jclass cls = (*env)->GetObjectClass(env, self);
  jmethodID method_id = (*env)->GetMethodID(env, cls, "update", "()V");
  if (method_id == 0) {
    return;
  }
  (*env)->CallVoidMethod(env, self, method_id);
  (*env)->DeleteLocalRef(env, cls);
}

JNIEXPORT void JNICALL Java_uk_co_halfninja_videokit_Videokit_setSize (JNIEnv *env, jobject self, jstring size)
{
  LOG_INFO("Let's throw an exception!");
  throwException(env, "Bam, not supported");
}

JNIEXPORT void JNICALL Java_uk_co_halfninja_videokit_Videokit_stop(JNIEnv* env, jobject self) {
  exiting = true;
}

JNIEXPORT void JNICALL Java_uk_co_halfninja_videokit_Videokit_doStuff(JNIEnv* env, jobject self, jobject bitmap) {
  exiting = false;

  AndroidBitmapInfo info;
  int bitmap_ret = AndroidBitmap_getInfo(env, bitmap, &info);
  if (bitmap_ret < 0) {
    LOG_INFO("Could not get bitmap info");
    return;
  }

  if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
    LOG_INFO("Bitmap format is not RGB_565");
    return;
  }

  AVFormatContext* format_context = avformat_alloc_context();

  const char* filename = "applehttp://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";

  int err = 0;
  if ((err = avformat_open_input(&format_context, filename, NULL, NULL)) != 0) {
    LOG_INFO("Can't open stream");
    char buf[256];
    av_strerror(err, buf, 256);
    LOG_INFO(buf);
    return;
  }

  if (av_find_stream_info(format_context) < 0) {
    LOG_INFO("Can't find stream info");
    return;
  }

  __android_log_print(ANDROID_LOG_INFO, "VideoKit", "Found %d streams", format_context->nb_streams);

  int video_stream = -1;
  for (unsigned int i = 0; i < format_context->nb_streams; ++i) {
    if (format_context->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
      video_stream = i;
      break;
    }
  }

  AVCodecContext* codec_context = format_context->streams[video_stream]->codec;
  AVCodec* codec = avcodec_find_decoder(codec_context->codec_id);
  if (!codec) {
    LOG_INFO("Could not find codec");
    return;
  }

  if (codec->capabilities & CODEC_CAP_TRUNCATED) {
    codec_context->flags |= CODEC_FLAG_TRUNCATED;
  }

  if (avcodec_open(codec_context, codec) > 0) {
    LOG_INFO("Could not open codec");
    return;
  }

  __android_log_print(ANDROID_LOG_INFO, "VideoKit", "Found %s codec", codec->name);

  AVFrame* yuv_frame = avcodec_alloc_frame();
  AVFrame* rgb_frame = avcodec_alloc_frame();
  AVPacket packet;
  while (av_read_frame(format_context, &packet) >= 0) {
    if (packet.stream_index == video_stream) {
      LOG_INFO("Got packet");
      __android_log_print(ANDROID_LOG_INFO, "VideoKit", "AVPackaet: %x", packet.data);
      int frame_finished = 0;
      int ret = avcodec_decode_video2(codec_context, yuv_frame, &frame_finished, &packet);
      __android_log_print(ANDROID_LOG_INFO, "VideoKit", "Decode: %d", ret);

      if (frame_finished) {
        LOG_INFO("Got complete frame");

        struct SwsContext* sws_context = sws_getCachedContext(
            NULL,
            codec_context->width,
            codec_context->height,
            codec_context->pix_fmt,
            codec_context->width,
            codec_context->height,
            PIX_FMT_RGB565,
            SWS_BICUBIC,
            NULL, NULL, NULL);
        if (!sws_context) {
          LOG_INFO("No swscaler");
          return;
        }

        void* pixels = NULL;
        bitmap_ret = AndroidBitmap_lockPixels(env, bitmap, &pixels);
        if (bitmap_ret < 0) {
          LOG_INFO("Could not lock pixels");
          return;
        }
        avpicture_fill((AVPicture*)rgb_frame, pixels, PIX_FMT_RGB565,
            codec_context->width, codec_context->height);

        //__android_log_print(ANDROID_LOG_INFO, "VideoKit", "Size: %dx%d",
        //    codec_context->width, codec_context->height);

        const uint8_t* const* input_data = (const uint8_t* const*)yuv_frame->data;
        sws_scale(
            sws_context,
            input_data,
            yuv_frame->linesize,
            0,
            codec_context->height,
            ((AVPicture*)rgb_frame)->data,
            ((AVPicture*)rgb_frame)->linesize);

        AndroidBitmap_unlockPixels(env, bitmap);

        LOG_INFO("Frame rendered");
        update(env, self);

        if (exiting) {
          break;
        }
      }
      av_free_packet(&packet);
    }
  }
}















