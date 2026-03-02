#!/bin/bash
_DIR_="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

DIR_SOURCE="${_DIR_}/src/main/jni"


echo "Warning: please install 'cmake automake make libtool gcc' in your OS first"

pushd "${DIR_SOURCE}"
    git submodule update --init --recursive librime snappy

    patch -p1 -d librime-lua-deps < patches/lua.patch
popd
