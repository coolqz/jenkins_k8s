def call(){
    def tools = new org.devops.tools()
    def requestdockerfile = libraryResource 'org/javademo/dockerfile/dockerfile'
    def requestyaml = libraryResource 'org/javademo/yaml/javademo.yaml'

    pipeline {
        agent {
            kubernetes {
                label "jenkins-slave"
                yaml '''
        apiVersion: v1
        kind: Pod
        metadata:
            name: jenkins-slave
        spec:
          containers:
          - name: jnlp
            image: "192.168.100.203/library/jenkins-jnlp-slave:jdk11"

          - name: maven
            image: "192.168.100.203/library/maven:3.8.6"
            command:
              - "cat"
            tty: true
            volumeMounts:
              - name: docker-cmd
                mountPath: /usr/local/bin/docker
              - name: docker-sock
                mountPath: /var/run/docker.sock
              - name: maven-cache
                mountPath: /root/.m2
          volumes:
            - name: docker-cmd
              hostPath:
                path: /usr/local/bin/docker
            - name: docker-sock
              hostPath:
                path: /var/run/docker.sock
            - name: maven-cache
              hostPath:
                path: /root/.m2
        '''
            }
       }

        options {
            buildDiscarder logRotator(
                artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '3', numToKeepStr: '5'
            )
            disableResume()
            disableConcurrentBuilds()
            skipDefaultCheckout true
            retry(1)
        }

        parameters {
            listGitBranches branchFilter: 'refs/heads/.*', 
            credentialsId: '46bc0911-8468-4171-b347-aaad153d5111', 
            defaultValue: '', 
            description: '请选择分支：',
            listSize: '5', 
            name: 'FROM_BRANCH', 
            quickFilterEnabled: false, 
            remoteURL: 'http://192.168.100.200/test/javademo.git', 
            selectedValue: 'NONE', 
            sortMode: 'NONE', 
            tagFilter: '*', 
            type: 'PT_BRANCH'
        }

        stages {
            stage('checkout_code'){
                steps {
                    container(name: 'maven'){
                        script {
                            tools.checkoutcode()
                        }
                    }
                }
            }

            stage('maven_build'){
                steps {
                    container(name: 'maven') {
                        sh """
                            mvn clean package -Dmaven.test.skip=true
                        """
                    }
                }
            }

            stage('image_build'){
                steps {
                    container(name: 'maven') {
                        script{
                            tools.writefile('dockerfile', requestdockerfile)
                            tools.harborlogin()
                        }
                        sh """
                            docker build -t 192.168.100.203/test/javademo:v1 .
                            docker push 192.168.100.203/test/javademo:v1
                        """
                    }
                }
            }

            stage('service_deploy'){
                steps {
                    container(name: 'maven') {
                        script{
                            tools.writefile('javademo.yaml', requestyaml)
                            tools.servicedeploy()
                        }
                    }                   
                }
            }
        }
    }
}