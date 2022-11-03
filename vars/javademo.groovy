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
            image: "192.168.100.203/library/jenkins-jnlp-slave:maven"
            volumeMounts:
              - name: docker-cmd
                mountPath: /usr/bin/docker
              - name: docker-sock
                mountPath: /var/run/docker.sock
          volumes:
            - name: docker-cmd
              hostPath: /usr/bin/docker
            - name: docker-sock
              hostPath: /var/run/docker.sock
        '''
            }
       }
 
        stages {
            stage('checkout_code'){
                steps {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/master']],
                        extensions: [],
                        userRemoteConfigs: [[
                            credentialsId: '46bc0911-8468-4171-b347-aaad153d5111', 
                            url: 'http://192.168.100.200/test/javademo.git'
                            ]]
                        ])
                }
            }

            stage('maven_build'){
                steps {
                    sh """
                        mvn clean package -Dmaven.test.skip=true
                    """
                }
            }

            stage('image_build'){
                steps {
                    script{
                        tools.writefile('dockerfile', requestdockerfile)
                        tools.harborlogin()
                    }
                    sh """
                        docker build -t javademo:v1 .
                        docker tag javademo:v1 192.168.100.203/test/javademo:v1
                        docker push 192.168.100.203/test/javademo:v1
                    """
                }
            }

            stage('service_deploy'){
                steps {
                    script{
                        tools.writefile('javademo.yaml', requestyaml)
                        tools.servicedeploy()
                    }                   
                }
            }
        }
    }
}