#!/bin/bash
pushd `dirname $0`
. settings.sh
pushd ffmpeg
make -j9
popd; popd
