#!/bin/sh
#
# runs codeclimate in hopefully the same way the gitlab autodevops does
#
# This is for developers if they want to run codeclimate locally
#
# Commands came from https://github.com/codeclimate/codeclimate
docker pull codeclimate/codeclimate
docker run   --interactive --tty --rm   --env CODECLIMATE_CODE="$PWD"   --volume "$PWD":/code   --volume /var/run/docker.sock:/var/run/docker.sock   --volume /tmp/cc:/tmp/cc   codeclimate/codeclimate analyze
