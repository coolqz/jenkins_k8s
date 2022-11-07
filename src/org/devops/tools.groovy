package org.devops

//writefile
def writefile(filename,content){                             
  writeFile encoding: 'UTF-8', file: "${filename}", text: "${content}" 
}

def harborlogin(){
    withCredentials([
        usernamePassword(
            credentialsId: '84d8aa3c-d320-4fa2-ba4d-910894080cf5', 
            passwordVariable: 'password', 
            usernameVariable: 'username'
        )
    ]) 
    sh """
        docker login 192.168.100.203 -u $username -p $password
    """
}

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