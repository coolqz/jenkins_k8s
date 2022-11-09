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


