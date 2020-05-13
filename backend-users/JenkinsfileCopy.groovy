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
        echo "Build"
        sh "mvn -Dmaven.test.skip=true compile"
      }
    }

    stage("Unit Test"){
      steps{
        echo "Unit Test"
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