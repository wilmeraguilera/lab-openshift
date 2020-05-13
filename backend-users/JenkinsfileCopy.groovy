pipeline{
  agent any

  tools{
    maven 'M2-3.6.3'
    jdk 'JDK18'
  }

  stages{
    stage("Checkout Source Code"){
      steps{
        echo "Checkout Source Code"
        checkout scm
      }
    }

    stage("Build"){
      steps{
        echo "Init Build"
        //Only apply the next instruction if you have the code in a subdirectory
        dir("backend-users"){
          sh "mvn -Dmaven.test.skip=true compile"
        }
        echo "End Build"
      }
    }

    stage("Unit Test"){
      steps{
        echo "Init Unit Test"
        echo "End Unit Test"
      }
    }

    stage("Execute Sonar analysis"){
      steps{
        echo "Execute Sonar analysis"
      }
    }

    stage("Publish to Nexus"){
      steps{
        echo "Publish to Nexus"
      }
    }

    stage("Build Image"){
      steps{
        echo "Build Image"
      }
    }

    stage("Deploy DEV"){
      steps{
        echo "Deploy DEV"
      }
    }
  }
}

def replaceValuesInFile (String templatePathFile, String finalPathFile, String propertiesPathFile){
  //Read properties file
  Properties props = new Properties()
  File propsFile = new File(propertiesPathFile)
  props.load(propsFile.newDataInputStream())
  Set values = props.entrySet()

  //read template
  File file = new File(templatePathFile)

  //Copiar el contenido del template al archivo definitivo
  Files.copy(file.toPath(), new File(finalPathFile).toPath(), StandardCopyOption.REPLACE_EXISTING)

  def destFile = new File('/home/wilmeraguilera/Documents/application-dev.properties')

  def content = destFile.text

  values.each { property ->
    content= content.replace('#{'+property.key+'}', property.value)
  }

  //Escribir archivo destino
  destFile.text = content


}