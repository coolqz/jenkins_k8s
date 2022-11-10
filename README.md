# script

# 1.说明

```
此代码库为jenkins以声明式流水线的扩展共享库的形式保存在git代码库中
测试代码为：https://github.com/coolqz/javademo.git
使用脚本库直接在jenkinsfile的变量中根据实际信息修改变量参数，在groovy文件中的parameters根据实际修改
```

## 1.1.流程图

![image-20221110163311268](https://github.com/coolqz/jenkins_k8s/blob/main/readme/image-20221110163311268.png)

## 1.2.组件说明

```
###jenkins
版本：Jenkins 2.361.2

使用到的插件：
Git
List Git Branches Parameter
SSH Pipeline Steps
Pipeline
Pipeline: Stage View
Pipeline: Groovy Libraries
kubernetes
Config File Provider
build user vars
```

## 1.3.镜像说明

```
#jenkins-slave镜像
Dockerfile
FROM openjdk:11-jre-slim-bullseye

RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    apt-get update && apt-get install -y git && \
    rm -rf /var/lib/apt/lists/* && \ 
    mkdir /usr/share/jenkins

COPY agent.jar /usr/share/jenkins/agent.jar
COPY jenkins-agent /usr/bin/jenkins-agent

#agent.jar可以在部署好的jenkins下载：http://jenkinsip:端口/jnlpJars/agent.jar
#jenkins-agent可以在https://github.com/jenkinsci/docker-inbound-agent下载
#注意：jenkins官方宣布Jenkins版本从2.357开始及后面版本，仅支持Java 11及以上版本

#maven镜像
FROM maven:3.8.6-openjdk-8-slim 

RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    apt-get update && apt-get -y install git && \
    rm -rf /var/lib/apt/lists/*

COPY settings.xml /usr/share/maven/conf/settings.xml
COPY kubectl /usr/bin/kubectl
```

