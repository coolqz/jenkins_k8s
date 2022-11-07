package org.devops

def checkoutcode(){
    checkout([
        $class: 'GitSCM',
        branches: [[name: "${FROM_BRANCH}"]],
        extensions: [],
        userRemoteConfigs: [[
            credentialsId: '46bc0911-8468-4171-b347-aaad153d5111', 
            url: 'http://192.168.100.200/test/javademo.git'
        ]]
    ])
}



//资源文件获取
def writefile(filename,content){                             
  writeFile encoding: 'UTF-8', file: "${filename}", text: "${content}" 
}

//登录harbor
def harborlogin(){
    withCredentials([
        usernamePassword(
            credentialsId: '84d8aa3c-d320-4fa2-ba4d-910894080cf5', 
            passwordVariable: 'password', 
            usernameVariable: 'username'
        )
    ]){
        sh """
            echo $password |docker login 192.168.100.203 -u $username --password-stdin
        """
    }
}

//服务部署
def servicedeploy(){
    kubeconfig(
        credentialsId: 'f2c47258-5493-428f-a102-c6ebaa012ff3', 
        serverUrl: 'https://192.168.100.10:6443'
        ) 
    {
        sh """
            kubectl apply -f javademo.yaml
        """
    }
}

//清除镜像和容器
def cleancontainerandimage(){
    sh """
        docker image prune -af && docker container prune -f
    """
}