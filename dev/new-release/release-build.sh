#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SELF=$(cd $(dirname $0) && pwd)
. "$SELF/release-util.sh"

function exit_with_usage {
  cat << EOF
usage: release-build.sh <package|docs|publish-snapshot|publish-release>
Creates build deliverables from a Spark commit.
Top level targets are
  package: Create binary packages and commit them to dist.apache.org/repos/dist/dev/spark/
  docs: Build docs and commit them to dist.apache.org/repos/dist/dev/spark/
  publish-snapshot: Publish snapshot release to Apache snapshots
  publish-release: Publish a release to Apache release repo
All other inputs are environment variables
GIT_REF - Release tag or commit to build from
SPARK_PACKAGE_VERSION - Release identifier in top level package directory (e.g. 2.1.2-rc1)
SPARK_VERSION - (optional) Version of Spark being built (e.g. 2.1.2)
ASF_USERNAME - Username of ASF committer account
ASF_PASSWORD - Password of ASF committer account
GPG_KEY - GPG key used to sign release artifacts
GPG_PASSPHRASE - Passphrase for GPG key
EOF
  exit 1
}

set -e

if [ $# -eq 0 ]; then
  exit_with_usage
fi

if [[ $@ == *"help"* ]]; then
  exit_with_usage
fi

if [[ -z "$ASF_PASSWORD" ]]; then
  echo 'The environment variable ASF_PASSWORD is not set. Enter the password.'
  echo
  stty -echo && printf "ASF password: " && read ASF_PASSWORD && printf '\n' && stty echo
fi

if [[ -z "$GPG_PASSPHRASE" ]]; then
  echo 'The environment variable GPG_PASSPHRASE is not set. Enter the passphrase to'
  echo 'unlock the GPG signing key that will be used to sign the release!'
  echo
  stty -echo && printf "GPG passphrase: " && read GPG_PASSPHRASE && printf '\n' && stty echo
fi

for env in ASF_USERNAME GPG_PASSPHRASE GPG_KEY; do
  if [ -z "${!env}" ]; then
    echo "ERROR: $env must be set to run this script"
    exit_with_usage
  fi
done

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

# Commit reference to checkout for build
GIT_REF=${GIT_REF:-master}

RELEASE_STAGING_LOCATION="https://dist.apache.org/repos/dist/dev/systemds"

GPG="gpg -u $GPG_KEY --no-tty --batch --pinentry-mode loopback"
NEXUS_ROOT=https://repository.apache.org/service/local/staging
# Profile for SystemDS staging uploads
# For example, Spark staging uploads it is `d63f592e7eac0`
NEXUS_PROFILE=
BASE_DIR=$(pwd)

init_java

rm -rf systemds
git clone "$ASF_REPO"
cd systemds
git checkout $GIT_REF
git_hash=`git rev-parse --short HEAD`
export GIT_HASH=$git_hash
echo "Checked out SystemDS git hash $git_hash"

DEST_DIR_NAME="$SPARK_PACKAGE_VERSION"

git clean -d -f -x
rm -f .gitignore
cd ..

if [[ "$1" == "package" ]]; then
  # Source and binary tarballs
  echo "Packaging release source tarballs"
  cp -r systemds systemds-$SYSTEMDS_VERSION
  
  # For excluding binary license/notice
  # if [[ $SYSTEMDS_VERSION > "2.0.0" ]]; then
  #   rm -f systemds-$SYSTEMDS_VERSION/LICENSE-binary
  #   rm -f systemds-$SYSTEMDS_VERSION/NOTICE-binary
  #   rm -rf systemds-$SYSTEMDS_VERSION/licenses-binary
  # fi
  
  tar cvzf systemds-$SYSTEMDS_VERSION.tgz --exclude systemds-$SYSTEMDS_VERSION/.git systemds-$SYSTEMDS_VERSION
  echo $GPG_PASSPHRASE | $GPG --passphrase-fd 0 --armour --output systemds-$SYSTEMDS_VERSION.tgz.asc \
    --detach-sig systemds-$SYSTEMDS_VERSION.tgz
  shasum -a 512 systemds-$SYSTEMDS_VERSION.tgz > systemds-$SYSTEMDS_VERSION.tgz.sha512
  rm -rf systemds-$SYSTEMDS_VERSION
  
  make_binary_release() {
    NAME=$1
    FLAGS="$MVN_EXTRA_OPTS -B $2"
    BUILD_PACKAGE=$3
    
    PIP_FLAG="--pip"
    
    echo "Building binary dist $NAME"
    cp -r systemds systemds-$SYSTEMDS_VERSION-bin-$NAME
    cd systemds-$SYSTEMDS_VERSION-bin-$NAME
    
    echo "Creating distribution: $NAME ($FLAGS)"
    
    # Write out the version to systemds version info
    # We rewrite the - to a . and
    # SNAPSHOT to dev0 to be closer to PEP440
    SYSTEMDS_VERSION_PY=`echo "$SYSTEMDS_VERSION" | sed -e "s/-/./" -e "s/SNAPSHOT/dev0/" -e "s/preview/dev/"`
    echo "__version__='$SYSTEMDS_VERSION_PY'" > python/systemds/version.py
    
    # Get maven home
    MVN_HOME=`mvn -version 2>&1 | grep 'Maven home' | awk '{print $NF}'`
    
    echo "Creating distribution"
    ./dev/make-distribution.sh --name $NAME --mvn $MVN_HOME/bin/mvn --tgz \
      $PIP_FLAG $R_FLAG $FLAGS 2>&1 >  ../binary-release-$NAME.log
    cd ..
    
    echo "Copying and signing python distribution"
    PYTHON_DIST_NAME=systemds-$SYSTEMDS_VERSION.tar.gz
    cp systemds-$SYSTEMDS_VERSION-bin-$NAME/python/dist/$PYTHON_DIST_NAME .
    
    echo $GPG_PASSPHRASE | $GPG --passphrase-fd 0 --armour \
      --output $PYTHON_DIST_NAME.asc \
      --detach-sig $PYTHON_DIST_NAME
    echo $GPG_PASSPHRASE | $GPG --passphrase-fd 0 --print-md \
      SHA512 systemds-$SYSTEMDS_VERSION-bin-$NAME.tgz > \
      systemds-$SYSTEMDS_VERSION-bin-$NAME.tgz.sha512
      
    
    
  }

  exit 0
fi

