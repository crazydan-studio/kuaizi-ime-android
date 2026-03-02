#!/bin/bash
_DIR_="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"


OCI_IMAGE="registry.gitlab.com/fdroid/docker-executable-fdroidserver:master"

DIR_ROOT="$(cd "${_DIR_}/.." && pwd -P)"
DIR_MAVEN_REPO="$(cd ~/.m2/repository && pwd -P)"
DIR_ANDROID_SDK="$(grep -E '^sdk.dir' "${DIR_ROOT}/local.properties" | awk -F= '{ print $2 }')"

GRADLE_VERSION="$(grep -E '^distributionUrl' "${DIR_ROOT}/gradle/wrapper/gradle-wrapper.properties" | sed 's|/|\n|g' | tail -n 1 | sed 's|gradle-||g; s|-bin.zip||g')"
DIR_GRADLE_HOME="$(cd ~/.gradle && pwd -P)"
DIR_GRADLE_BIN="$(find "${DIR_GRADLE_HOME}/wrapper/dists" -type d -name "gradle-${GRADLE_VERSION}" | tail -n 1)"

NDK_VERSION="$(ls -1 "${DIR_ANDROID_SDK}/ndk" | tail -n 1)"
APP_ID="$(grep -E '^\s+applicationId\b' "${DIR_ROOT}/app/build.gradle" | sed 's/[="]/ /g' | sed "s/'//g" | awk '{ print $2 }')"
APP_VERSION_NAME="$(grep -E '^\s+versionName\b' "${DIR_ROOT}/app/build.gradle" | sed 's/[="]/ /g' | sed "s/'//g" | awk '{ print $2 }')"
APP_VERSION_CODE="$(grep -E '^\s+versionCode\b' "${DIR_ROOT}/app/build.gradle" | sed 's/[="]/ /g' | sed "s/'//g" | awk '{ print $2 }')"

FILE_FDROID_APP_CONFIG="${DIR_ROOT}/.fdroid/app.yml"
DIR_FDROID_BUILD="${DIR_ROOT}/build/fdroid"


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


mkdir -p "${DIR_FDROID_BUILD}"/{metadata,repo,build}

if [ ! -e "${DIR_FDROID_BUILD}/config.yml" ]; then
    $OCI_EXE run --rm \
        -e ANDROID_HOME=/opt/android-sdk \
        -v "${DIR_ANDROID_SDK}:/opt/android-sdk" \
        -v "${DIR_FDROID_BUILD}:/repo" \
        ${OCI_IMAGE} \
        init -v \
    || exit 1
fi

sed "s|{{version_name}}|${APP_VERSION_NAME}|g; s|{{version_code}}|${APP_VERSION_CODE}|g; s|{{ndk_version}}|${NDK_VERSION}|g" \
    "${FILE_FDROID_APP_CONFIG}" \
> "${DIR_FDROID_BUILD}/metadata/${APP_ID}.yml"

$OCI_EXE run --rm \
    -e ANDROID_HOME=/opt/android-sdk \
    -v "${DIR_ANDROID_SDK}:/opt/android-sdk" \
    -v "${DIR_GRADLE_HOME}:/home/vagrant/.gradle" \
    -v "${DIR_GRADLE_BIN}:/root/.cache/gradlew-fdroid/versions/${GRADLE_VERSION}" \
    -v "${DIR_MAVEN_REPO}:/root/.m2/repository" \
    -v "${DIR_FDROID_BUILD}:/repo" \
    ${OCI_IMAGE} \
    build -v -l ${APP_ID}
