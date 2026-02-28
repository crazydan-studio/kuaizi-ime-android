#!/bin/bash
_DIR_="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

DCR_IMAGE="studio.crazydan.org/kuaizi-ime-cross:android"

DIR_SOURCE="${_DIR_}/src/main/jni"
DIR_OUTPUT="${_DIR_}/src/main/jniLibs"


if [ -z "$OCI_EXE" ]; then
    if type -p podman >/dev/null 2>/dev/null; then
        OCI_EXE=podman
    elif type -p docker >/dev/null 2>/dev/null; then
        OCI_EXE=docker
    else
        echo "Cannot find a container executor. Search for docker and podman."
        exit 1
    fi
fi

pushd "${DIR_SOURCE}"
    git submodule update --init --recursive librime snappy
    git apply --directory=librime-lua-deps patches/lua.patch
popd


archs=('arm64-v8a' 'x86_64')
for arch in "${archs[@]}"; do
    output="${DIR_OUTPUT}/${arch}"
    mkdir -p "${output}"

    $OCI_EXE run --rm -it \
        -v "${DIR_SOURCE}:/source:ro" \
        -v "${output}:/output" \
        ${DCR_IMAGE} \
        bash -c " \
            ln -s /deps/boost-\${BOOST_VERSION}.tar.xz \
                boost-\${BOOST_VERSION}.tar.xz \
            && cmake -H/source \
                -DCMAKE_SYSTEM_NAME=Android \
                -DCMAKE_BUILD_TYPE=Release \
                -DCMAKE_ANDROID_STL_TYPE=c++_static \
                -DCMAKE_SYSTEM_VERSION=14 \
                -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
                -DCMAKE_ANDROID_ARCH_ABI=${arch} \
            && make -j4 rime_jni \
            && mv librime_jni/librime_jni.so /output \
        "
done

uid="$(id -u)"
gid="$(id -g)"
sudo chown $uid:$gid -R "${DIR_OUTPUT}"
