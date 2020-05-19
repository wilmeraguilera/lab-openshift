//Variables del proceso
def tagImage
def artifactName
def artifactVersion
def nameJar

pipeline {

  agent any

  //agent {
  //    label "maven-appdev"
  //}

  environment {
    //maven 'M2-3.6.3'
    MAVEN_HOME = tool('M2-3.6.3')
    JAVA_HOME = tool('JDK18')
    PATH = "$PATH:$JAVA_HOME:$MAVEN_HOME"

  }

  parameters {
    string(name: 'namespace_dev', defaultValue: 'dev-admin-users', description: 'Nombre del proyecto en Openshift para DEV')
    string(name: 'namespace_qa', defaultValue: 'qa-admin-users', description: 'Nombre del proyecto en Openshift para QA')
    string(name: 'namespace_prod', defaultValue: 'prod-admin-users', description: 'Nombre del proyecto en Openshift para PROD')
    string(name: 'appName', defaultValue: 'api-users', description: 'Nombre de la aplicación')
  }

  stages {
    stage("Checkout Source Code") {
      steps {
        echo "Init Checkout Source Code"
        checkout scm
        script {
          echo "PATH is: $PATH"
          echo "JAVA is: $JAVA_HOME"
          echo "Path: ${PATH}"
          sh 'java -version'


          dir("backend-users") {
            //Obtener version del artefacto
            def pom = readMavenPom file: 'pom.xml'
            tagImage = pom.version + "-" + currentBuild.number

            artifactName = pom.artifactId
            artifactVersion = pom.version
            nameJar = artifactName + "-" + artifactVersion + ".jar"
          }
        }
        echo "end Checkout Source Code"
      }
    }
    
    stage("Checkout config"){
      steps{
        sh "mkdir -p config-files"
        dir("config-files"){
          git credentialsId: 'git-wilmer', url: 'https://github.com/wilmeraguilera/lab-openshift-config.git'
        }
      }
    }

    stage("Build") {
      steps {
        echo "Init Build"
        //Only apply the next instruction if you have the code in a subdirectory
        dir("backend-users") {
          sh "mvn -Dmaven.test.skip=true compile"
        }
        echo "End Build"
      }
    }

    stage("Unit Test") {
      steps {
        echo "Init Unit Test"
        dir("backend-users") {
          sh "mvn test"
        }
        echo "End Unit Test"
      }
    }


    stage('SonarQube Scan') {
      steps {
        dir("backend-users") {
          withSonarQubeEnv('sonar') {
            sh "mvn sonar:sonar " +
                    "-Dsonar.java.coveragePlugin=jacoco -Dsonar.junit.reportsPath=target/surefire-reports  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml "
          }
          sleep(10)
          timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
          }
        }
      }

    }

    stage("Publish to Nexus") {
      steps {
        echo "Init Publish to Nexus"
        dir("backend-users") {
          sh "mvn deploy -DskipTests=true -s ./configuration/settings-maven.xml"
        }
        echo "End Publish to Nexus"
      }
    }

    stage("Build Image") {
      steps {
        script {
          dir("backend-users") {
            openshift.withCluster() {
              openshift.withProject("${params.namespace_dev}") {
                openshift.selector("bc", "${params.appName}").startBuild("--from-file=./target/${nameJar}", "--wait=true")
                openshift.tag("${params.appName}:latest", "${params.appName}:${tagImage}")
              }
            }
          }
        }
      }
    }

    stage("Deploy DEV") {
      steps {
        script {
          //Crear archivo de propiedades dev
          replaceValuesInFile('config-files/backend-users/config-dev.properties', 'backend-users/src/main/resources/application-env.properties', 'backend-users/src/main/resources/application.properties')

          dir("backend-users") {
            openshift.withCluster() {
              openshift.withProject("${params.namespace_dev}") {

                openshift.selector('configmap', 'config-backend-users').delete(' --ignore-not-found=true ')
                openshift.create('configmap', 'config-backend-users', '--from-file=./src/main/resources/application.properties')
                openshift.set("image", "dc/${params.appName}", "${params.appName}=${params.namespace_dev}/${params.appName}:${tagImage}", " --source=imagestreamtag")
                openshift.selector("dc", "${params.appName}").rollout().latest();
                sleep 2

                // Wait for application to be deployed
                def dc = openshift.selector("dc", "${params.appName}").object()
                def dc_version = dc.status.latestVersion
                def rc = openshift.selector("rc", "${params.appName}-${dc_version}").object()
                echo "Waiting for ReplicationController ${params.appName}-${dc_version} to be ready"
                while (rc.spec.replicas != rc.status.readyReplicas) {
                  sleep 10
                  rc = openshift.selector("rc", "${params.appName}-${dc_version}").object()
                }
              }
            }
          }
        }
      }
    }

    stage("Deploy QA") {
      steps {
        script {
          //Crear archivo de propiedades dev
          replaceValuesInFile('config-files/backend-users/config-qa.properties', 'backend-users/src/main/resources/application-env.properties', 'backend-users/src/main/resources/application.properties')

          dir("backend-users") {
            openshift.withCluster() {
              openshift.withProject("${params.namespace_qa}") {

                openshift.selector('configmap', 'config-backend-users').delete(' --ignore-not-found=true ')
                openshift.create('configmap', 'config-backend-users', '--from-file=./src/main/resources/application.properties')
                openshift.set("image", "dc/${params.appName}", "${params.appName}=${params.namespace_dev}/${params.appName}:${tagImage}", " --source=imagestreamtag")

                // Deploy the development application.
                openshift.selector("dc", "${params.appName}").rollout().latest();
                sleep 2

                // Wait for application to be deployed
                def dc = openshift.selector("dc", "${params.appName}").object()
                def dc_version = dc.status.latestVersion
                def rc = openshift.selector("rc", "${params.appName}-${dc_version}").object()

                echo "Waiting for ReplicationController ${params.appName}-${dc_version} to be ready"
                while (rc.spec.replicas != rc.status.readyReplicas) {
                  sleep 10
                  rc = openshift.selector("rc", "${params.appName}-${dc_version}").object()
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Metodo encargado de leer una archivo de propiedades y reemplazar los valores en en achivo destino.
 *
 * En el archivo destino se buscan comodides de la estructura ${var}*
 * @param valuesPropertiesFile
 * @param templateFile
 * @param destinationFile
 * @return
 */
def replaceValuesInFile(valuesPropertiesFile, templateFile, destinationFile) {
  def props = readProperties file: valuesPropertiesFile

  def textTemplate = readFile templateFile
  echo "Contenido leido del template: " + textTemplate

  props.each { property ->
    echo property.key
    echo property.value
    textTemplate = textTemplate.replace('${' + property.key + '}', property.value)
  }

  echo "Contenido Reemplazado: " + textTemplate

  finalText = textTemplate
  writeFile(file: destinationFile, text: finalText, encoding: "UTF-8")
}

