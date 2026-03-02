#!/bin/bash
_DIR_="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

pushd "${_DIR_}"
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
    ./gradlew assembleAlpha
popd
