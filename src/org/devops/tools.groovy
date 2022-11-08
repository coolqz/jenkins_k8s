package org.devops

def checkoutcode(CODE_AUTH,CODE_ADDR){
    checkout([
        $class: 'GitSCM',
        branches: [[name: "${FROM_BRANCH}"]],
        extensions: [],
        userRemoteConfigs: [[
            credentialsId: "${CODE_AUTH}", 
            url: "${CODE_ADDR}"
        ]]
    ])
}



//资源文件获取
def writefile(filename,content){                             
  writeFile encoding: 'UTF-8', file: "${filename}", text: "${content}" 
}

//登录harbor
def harborlogin(HARBOR_AUTH){
    withCredentials([
        usernamePassword(
            credentialsId: "${HARBOR_AUTH}", 
            passwordVariable: 'password', 
            usernameVariable: 'username'
        )
    ]){
        sh """
            echo $password |docker login ${HARBOR} -u $username --password-stdin
        """
    }
}

//服务部署
def servicedeploy(){
    kubeconfig(
        credentialsId: "${K8S_AUTH}", 
        serverUrl: "${K8S_ADDR}"
        ) 
    {
        sh """
            kubectl apply -f javademo.yaml
        """
    }
}
