#!/bin/bash

t=`pwd`;
DOCKER=`basename "$t"`
echo "Docker: $DOCKER"

docker ps -a|grep $DOCKER|awk '{print $1}'|xargs docker rm
docker images|grep $DOCKER|awk '{print $1":"$2}'|xargs docker rmi


