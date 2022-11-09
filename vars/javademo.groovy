def call(Map map){
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
            PROJECT_NAME = "${map.PROJECT_NAME}"
            SERVICE_NAME = "${map.SERVICE_NAME}"
            HARBOR = "${map.HARBOR}"
            HARBOR_AUTH = "${map.HARBOR_AUTH}"
            GIT = "${map.GIT}"
            GIT_ADDR = "http://${GIT}/${PROJECT_NAME}/${SERVICE_NAME}.git"
            GIT_AUTH = "${map.GIT_AUTH}"
            K8S_ADDR = "${map.K8S_ADDR}"
            K8S_AUTH = "${map.K8S_AUTH}"
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
            credentialsId: "${GIT_AUTH}", 
            defaultValue: 'master', 
            description: '请选择分支：',
            listSize: '5', 
            name: 'FROM_BRANCH', 
            quickFilterEnabled: false, 
            remoteURL: "http://192.168.100.200/test/javademo.git", 
            selectedValue: 'DEFAULT', 
            sortMode: 'NONE', 
            tagFilter: '*', 
            type: 'PT_BRANCH'
        }

        stages {
            stage('get_message'){
                steps{
                    script{
                        tools.getbuildmsg()
                    }
                }
            }

            stage('checkout_code'){
                steps {
                    container(name: 'maven'){
                        checkout([
                            $class: 'GitSCM', branches: [[name: "${FROM_BRANCH}"]], extensions: [],
                            userRemoteConfigs: [[
                                credentialsId: "${GIT_AUTH}", url: "${GIT_ADDR}"
                            ]]
                        ])
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
                        }
                        withCredentials([
                            usernamePassword(
                                credentialsId: "${HARBOR_AUTH}",
                                passwordVariable: 'password',
                                usernameVariable: 'username')
                        ])
                        {
                            sh """
                                docker login ${HARBOR} -u $username -p $password
                                docker build -t ${HARBOR}/${PROJECT_NAME}/${SERVICE_NAME}:v1 .
                                docker push ${HARBOR}/${PROJECT_NAME}/${SERVICE_NAME}:v1
                            """
                        }
                    }
                }
            }

            stage('service_deploy'){
                steps {
                    container(name: 'maven') {
                        script{
                            tools.writefile('javademo.yaml', requestyaml)
                            kubeconfig(
                                credentialsId: "${K8S_AUTH}",
                                serverUrl: "${K8S_ADDR}")
                            {
                                sh """
                                    kubectl apply -f javademo.yaml
                                """
                            }
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