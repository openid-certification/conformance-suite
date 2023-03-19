#!/bin/bash

set -e

VERSION=$(git describe --tags HEAD)

echo Building ${VERSION}...

mvn clean package

TMPDIR=$(mktemp -d)

cp package/README.md $TMPDIR

mkdir -p $TMPDIR/server
cp target/fapi-test-suite.jar $TMPDIR/server
cp package/Dockerfile.server $TMPDIR/server/Dockerfile

cp -R httpd $TMPDIR
cp package/docker-compose.yml $TMPDIR

echo Archiving...

TARGET=${PWD}/conformance-suite-${VERSION}.zip

(cd $TMPDIR && zip -r $TARGET .)

rm -r $TMPDIR

echo Created archive

ls -l $TARGET
