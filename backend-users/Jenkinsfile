//Variables del proceso
def tagImage
def artifactName
def artifactVersion
def nameJar
def isRelease
def var_context_dir


def namespace_dev ="dev-admin-users"
def namespace_qa = "qa-admin-users"
def appName = "api-users"
def context_dir ="backend-users"




//Constantes
def CONFIG_FILE_NAME="application.properties"

/*
podTemplate(
  label: "maven-pod",
  cloud: "openshift",
  inheritFrom: "maven",
  containers: [
    containerTemplate(
      name: "jnlp",
      image: "docker-registry.default.svc:5000/jenkins/jenkins-agent-maven-35-rhel7",
      resourceRequestMemory: "0.5Gi",
      resourceLimitMemory: "0.5Gi",
      resourceRequestCpu: "0.25",
      resourceLimitCpu: "0.25"
    )
  ],
  volumes: [persistentVolumeClaim(claimName: 'jenkins-m2-repo-pvc', mountPath: '/home/jenkins/.m2')]
)*/

pipeline {

  agent any

  //agent {
  //    label "maven-appdev"
  //}

  tools {
    maven 'M2-3.6.3'
    jdk 'JDK18'
  }


  stages {

    stage("Checkout Source Code") {
      steps {
        echo "Init Checkout Source Code"
        checkout scm
        script {
          echo "Path: ${PATH}"
          echo "M2_HOME = ${M2_HOME}"
          sh 'java -version'


          //Calcular variable en caso de directorio interno en el git
          var_context_dir = "${context_dir}"
          if (var_context_dir?.trim()){
            var_context_dir = "${env.WORKSPACE}/${var_context_dir}";
          }else{
            var_context_dir = "${env.WORKSPACE}";
          }



          echo "Directorio: ${var_context_dir}"

          dir(var_context_dir) {
            //Obtener version del artefacto
            def pom = readMavenPom file: 'pom.xml'
            tagImage = pom.version + "-" + currentBuild.number

            artifactName = pom.artifactId
            artifactVersion = pom.version
            nameJar = artifactName + "-" + artifactVersion + ".jar"

            //Identificar si es snapshot o release
            isRelease = !artifactVersion.contains ('SNAPSHOT')

            if (isRelease){
                echo "Es version release: "+ artifactVersion
            }else{
                echo "Es version Snapshot: "+ artifactVersion
            }
          }
        }
        echo "end Checkout Source Code"
      }
    }


    stage("Build") {
      steps {
        echo "Init Build"
        //Only apply the next instruction if you have the code in a subdirectory
        dir(var_context_dir) {
          sh "mvn -Dmaven.test.skip=true compile -s ./configuration/settings-maven.xml "
        }
        echo "End Build"
      }
    }

    stage("Unit Test") {
      steps {
        echo "Init Unit Test"
        dir(var_context_dir) {
          sh "mvn test -s ./configuration/settings-maven.xml"
        }
        echo "End Unit Test"
      }
    }

/*
    stage('SonarQube Scan') {
      steps {
        dir(var_context_dir) {
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

    }*/

    stage("Publish to Nexus") {
      steps {
        echo "Init Publish to Nexus"
        dir(var_context_dir) {
          script {
              if (isRelease){
                sh "mvn -s ./configuration/settings-maven.xml deploy -DskipTests=true  -DaltDeploymentRepository=nexus::default::http://nexus3-nexus.apps-crc.testing/repository/maven-releases"
              }else{
                sh "mvn -s ./configuration/settings-maven.xml deploy -DskipTests=true  -DaltDeploymentRepository=nexus::default::http://nexus3-nexus.apps-crc.testing/repository/maven-snapshots"
              }
          }
        }
        echo "End Publish to Nexus"
      }
    }

    stage("Build Image") {
      steps {
        script {
          echo "Init Build Image"
          dir(var_context_dir) {
            openshift.withCluster() {
              openshift.verbose()
              openshift.withProject("${namespace_dev}") {
                openshift.selector("bc", "${appName}").startBuild("--from-file=./target/${nameJar}", "--wait=true")
                openshift.tag("${appName}:latest", "${appName}:${tagImage}")
              }
            }
          }
          echo "End Build Image"
        }
      }
    }

    stage("Deploy DEV") {
      steps {
        script {
          echo "Init Deploy Image to DEV Environment"
          dir(var_context_dir) {
            openshift.withCluster() {
                openshift.verbose()
                openshift.withProject("${namespace_dev}") {

                openshift.selector("configmap", "config-${appName}").delete(" --ignore-not-found=true " )
                openshift.create("configmap", "config-${appName}", "--from-file=./src/main/resources/${CONFIG_FILE_NAME}")
                openshift.set("image", "dc/${appName}", "${appName}=${namespace_dev}/${appName}:${tagImage}", " --source=imagestreamtag")
                openshift.selector("dc", "${appName}").rollout().latest();
                sleep 2

                // Wait for application to be deployed
                def dc = openshift.selector("dc", "${appName}").object()
                def dc_version = dc.status.latestVersion
                def rc = openshift.selector("rc", "${appName}-${dc_version}").object()
                echo "Waiting for ReplicationController ${appName}-${dc_version} to be ready"
                while (rc.spec.replicas != rc.status.readyReplicas) {
                  sleep 10
                  rc = openshift.selector("rc", "${appName}-${dc_version}").object()
                }
              }
            }
          }
          echo "End Deploy Image to DEV Environment"
        }
      }
    }

    stage("Deploy QA") {
      steps {
        script {
          echo "Init Deploy Image to QA Environment"
          dir(var_context_dir) {
            openshift.withCluster() {
              openshift.withProject("${namespace_qa}") {
                openshift.selector("configmap", "config-${appName}").delete(" --ignore-not-found=true ")
                openshift.create("configmap", "config-${appName}", "--from-file=./src/main/resources/${CONFIG_FILE_NAME}")
                openshift.set("image", "dc/${appName}", "${appName}=${namespace_dev}/${appName}:${tagImage}", " --source=imagestreamtag")

                // Deploy the development application.
                openshift.selector("dc", "${appName}").rollout().latest();
                sleep 2

                // Wait for application to be deployed
                def dc = openshift.selector("dc", "${appName}").object()
                def dc_version = dc.status.latestVersion
                def rc = openshift.selector("rc", "${appName}-${dc_version}").object()

                echo "Waiting for ReplicationController ${appName}-${dc_version} to be ready"
                while (rc.spec.replicas != rc.status.readyReplicas) {
                  sleep 10
                  rc = openshift.selector("rc", "${appName}-${dc_version}").object()
                }
              }
            }
          }
          echo "End Deploy Image to QA Environment"
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

