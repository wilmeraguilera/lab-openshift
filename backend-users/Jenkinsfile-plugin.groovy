// Jenkinsfile for Api-users - 
// Set the tag for the development image: version + build number
def devTag      = "0.0.0"

// Set the tag for the production image: version
def prodTag     = "0.0.0"
def artifactVersion
def artifactName	= ""
def destApp = "" 
def activeApp = ""

node() {
  properties([
      parameters(
          [
            string(name: 'namespace', defaultValue: 'dev-admin-users', description:'Nombre del proyecto en Openshift'),
            string(name: 'appName', defaultValue: 'api-users', description:'Application name')
          ]
        )
  ])

  echo "Namespace: ${params.namespace}"
  echo "Application name: ${params.appName}"

  //Stage de Preparación y configuración de herramientas
  stage('Preparing'){

    //Herramienta de Maven
		mvnHome = tool 'M2'
		mvnCmd = "${mvnHome}/bin/mvn "

    //Definición Jdk
		env.JAVA_HOME=tool 'JDK18'
		env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
		sh 'java -version'
	}
    
    
  // Stage descarga código fuente y leer variables del pom
  stage('Checkout Source') {
	  checkout scm  
    dir("backend-users"){
      //Obtener version y el nombre del artefacto 
      def pom = readMavenPom file: 'pom.xml'
      artifactVersion = pom.version
      artifactName = pom.artifactId
      echo "La version del artefacto es: "+artifactVersion; 
      echo "El nombre de artefacto es: "+artifactName; 

      devTag  = "${artifactVersion}-" + currentBuild.number
      echo "Versión de la imagen Devtag: ${devTag}"
    }
	        	
  }
  

  // Stage Build App
  stage('Build App') {
    dir("backend-users"){
      echo "Building version ${devTag}"
      sh "${mvnCmd} clean install -DskipTests -s ./configuration/settings-maven.xml"
      echo "Building complete version ${devTag}"
    }     
  }
    
  //Stage Ejecución de Pruebas unitarias
  stage('Unit Tests') {
    dir("backend-users"){      
			echo "Running Unit Tests"
  		sh "${mvnCmd}  test -s ./configuration/settings-maven.xml"
	  }  
  }

  //Stage de análisis estático de código con Sonar
  stage('SonarQube Scan') {
    dir("backend-users"){  
      echo "Init Running Code Analysis"
      withSonarQubeEnv('sonar') {
        sh "${mvnCmd} sonar:sonar " +
              "-Dsonar.java.coveragePlugin=jacoco -Dsonar.junit.reportsPath=target/surefire-reports  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml -s ./configuration/settings-maven.xml"
        
      }
      sleep(10)
          
      timeout(time: 1, unit: 'MINUTES') {
        def qg = waitForQualityGate()
        if (qg.status != 'OK') {
          error "Pipeline aborted due to quality gate failure: ${qg.status}"
        }
      }
      echo "End Running Code Analysis"
    }
  }

  //Public en un repository (Nexus)
	stage('Publish to Nexus') {
    dir("backend-users"){   
      echo "Publish to Nexus"
      sh "${mvnCmd}  deploy -DskipTests=true -s ./configuration/settings-maven.xml"
    }
	}


  //Stage Build Image
  stage('Create Image'){
    dir("backend-users"){
      openshift.withCluster(){
        openshift.withProject("${params.namespace}") {
          echo "Inicia creación image"
          echo devTag
          echo prodTag
          openshift.selector("bc", "${params.appName}").startBuild("--from-file=./target/${artifactName}-${artifactVersion}.jar", "--wait=true")
          openshift.tag("${params.appName}:latest", "${params.appName}:${devTag}")
          echo "Termina creación image"
        }
      }
    }
  }

  //Stage deploy Dev
  stage('Deploy to DEV'){
      dir("backend-users"){
        openshift.withCluster(){
          openshift.withProject("${params.namespace}") {

            //input 'Deploy?'
            echo "Inicia Deploy"

            openshift.selector('configmap', 'myconfigmap').delete(' --ignore-not-found=true ')
						openshift.create('configmap', 'myconfigmap', '--from-file=./src/main/resources/application.properties')
            openshift.set("image", "dc/${params.appName}", "${params.appName}=${params.namespace}/${params.appName}:${devTag}", " --source=imagestreamtag ")

            //sh "oc rollout latest dc/${params.appName} -n ${params.namespace}"
            openshift.selector("dc", "${params.appName}").rollout().latest();

            sleep 2

            // Wait for application to be deployed
            def dc = openshift.selector("dc", "${params.appName}").object()
            def dc_version = dc.status.latestVersion
            def rc = openshift.selector("rc", "${params.appName}-${dc_version}").object()

            echo "Waiting for ReplicationController ${params.appName}-${dc_version} to be ready"
            while (rc.spec.replicas != rc.status.readyReplicas) {
              sleep 5
              rc = openshift.selector("rc", "${params.appName}-${dc_version}").object()
            }
          }
        }
      }  
  }

}