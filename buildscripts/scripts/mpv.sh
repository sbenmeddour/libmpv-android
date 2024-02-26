#!/bin/bash -e

. ../../include/depinfo.sh
. ../../include/path.sh

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
    true
elif [ "$1" == "clean" ]; then
    rm -rf $build
    exit 0
else
    exit 255
fi

unset CC CXX # meson wants these unset

meson setup $build --cross-file "$prefix_dir"/crossfile.txt \
    --default-library shared \
    -Diconv=disabled \
    -Dlua=disabled \
    -Dlibmpv=true \
    -Dcplayer=false \
    -Dmanpage-build=disabled

ninja -C $build -j$cores
DESTDIR="$prefix_dir" ninja -C $build install

ln -sf "$prefix_dir"/lib/libmpv.so "$native_dir"
