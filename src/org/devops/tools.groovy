package org.devops

//获取构建信息
def getbuildmsg(){
    wrap([$class: 'BuildUser']){
        currentBuild.description = "Trigger by ${BUILD_USER}, Branch: ${FROM_BRANCH}"
    }
}

//资源文件获取
def writefile(filename,content){                             
  writeFile encoding: 'UTF-8', file: "${filename}", text: "${content}" 
}

//登录harbor
def harborlogin(){
    withCredentials([
        usernamePassword(
            credentialsId: "${map.HARBOR_AUTH}", 
            passwordVariable: 'password', 
            usernameVariable: 'username'
        )
    ]){
        sh """
            echo $password |docker login ${map.HARBOR} -u $username --password-stdin
        """
    }
}

//服务部署
def servicedeploy(){
    kubeconfig(
        credentialsId: "${map.K8S_AUTH}", 
        serverUrl: "${map.K8S_ADDR}"
        ) 
    {
        sh """
            kubectl apply -f javademo.yaml
        """
    }
}
