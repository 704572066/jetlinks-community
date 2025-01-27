#!/usr/bin/env bash

#dockerImage=registry.cn-hangzhou.aliyuncs.com/zju_namespace/jetlinks-community:$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
dockerImage=registry.cn-hangzhou.aliyuncs.com/zju_namespace/jetlinks-community:2.2.2

./mvnw clean package -Dmaven.test.skip=true -Dmaven.build.timestamp="$(date "+%Y-%m-%d %H:%M:%S")"
if [ $? -ne 0 ];then
    echo "构建失败!"
else
  cd ./jetlinks-standalone || exit
  docker build -t "$dockerImage" . && docker push "$dockerImage"
fi