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

        environment {
            PROJECT_NAME="test"
            SERVICE_NAME="javademo"
            HARBOR="192.168.100.203"
            HARBOR_AUTH=credentials('84d8aa3c-d320-4fa2-ba4d-910894080cf5')
            GITLAB="192.168.100.200"
            CODE_ADDR="192.168.100.200/test/javademo.git"
            CODE_AUTH=credentials('46bc0911-8468-4171-b347-aaad153d5111')
            K8S_ADDR="https://192.168.100.10:6443"
            K8S_AUTH=credentials('f2c47258-5493-428f-a102-c6ebaa012ff3')

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
            listGitBranches branchFilter: 'refs/heads/(.*)', 
            credentialsId: "${CODE_AUTH}", 
            defaultValue: 'master', 
            description: '请选择分支：',
            listSize: '5', 
            name: 'FROM_BRANCH', 
            quickFilterEnabled: false, 
            remoteURL: "${CODE_ADDR}", 
            selectedValue: 'DEFAULT', 
            sortMode: 'NONE', 
            tagFilter: '*', 
            type: 'PT_BRANCH'
        }

        stages {
            stage('set_message'){
                steps{
                    script{
                        wrap([$class: 'BuildUser']){
                            currentBuild.description = "Trigger by ${BUILD_USER}, Branch: ${FROM_BRANCH}"
                        }
                    }
                }
            }

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
                            docker build -t ${HARBOR}/${PROJECT_NAME}/${SERVICE_NAME}:v1 .
                            docker push ${HARBOR}/${PROJECT_NAME}/${SERVICE_NAME}:v1
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

        post {
            success {
                echo "发版成功，请查看服务运行情况"
            }
            failure {
                echo "发版失败，请查看输出日志排查原因"
            }
        }
    }
}