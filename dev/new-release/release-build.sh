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
