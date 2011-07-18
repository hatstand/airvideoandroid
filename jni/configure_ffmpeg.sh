#!/bin/bash
pushd `dirname $0`
. settings.sh

if [[ $minimal_featureset == 1 ]]; then
  echo "Using minimal featureset"
  featureflags="--disable-everything \
--enable-decoder=mjpeg --enable-demuxer=mjpeg --enable-parser=mjpeg \
--enable-muxer=mp4 \
--enable-decoder=rawvideo \
--enable-protocol=file \
--enable-hwaccels"
fi

pushd ffmpeg

./configure --enable-cross-compile \
--arch=arm \
--cpu=cortex-a9 \
--target-os=linux \
--disable-stripping \
--prefix=../output \
--disable-shared \
--enable-static \
--enable-memalign-hack \
--cc=arm-linux-androideabi-gcc \
--ld=arm-linux-androideabi-ld \
--extra-cflags="-fPIC -DANDROID -Wfatal-errors -Wno-deprecated" \
$featureflags \
--disable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-demuxer=v4l \
--disable-demuxer=v4l2 \
--disable-indev=v4l \
--disable-indev=v4l2 \
--enable-demuxer=h264 \
--enable-demuxer=m4v \
--enable-demuxer=mp3 \
--enable-demuxer=mpegts \
--enable-decoder=aac \
--enable-decoder=h264 \
--enable-decoder=mp3 \
--enable-protocol=applehttp \
--enable-protocol=http \
--enable-parser=h264 \
--enable-parser=mpeg4audio \
--enable-parser=mpeg4video \
--extra-cflags="-I../x264" \
--extra-ldflags="-L../x264" \
--extra-libs="-lgcc -L$NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/lib/gcc/arm-linux-androideabi/4.4.3" 

popd; popd
