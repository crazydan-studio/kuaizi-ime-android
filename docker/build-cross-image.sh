#!/bin/bash
_DIR_="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

DCR_IMAGE="studio.crazydan.org/kuaizi-ime-cross:android"

docker build \
    -t ${DCR_IMAGE} \
    -f Dockerfile.cross \
    "${_DIR_}"
