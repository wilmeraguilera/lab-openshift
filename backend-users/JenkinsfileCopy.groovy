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

    tools {
        maven 'M2-3.6.3'
        jdk 'JDK18'
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
                echo "Checkout Source Code"
                sh "mkdir config-files"
                dir("code-app") {
                    checkout scm
                }
                dir("code-app/backend-users") {
                    script {
                        //Obtener version del artefacto
                        def pom = readMavenPom file: 'pom.xml'
                        tagImage = pom.version + "-" + currentBuild.number

                        artifactName = pom.artifactId
                        artifactVersion = pom.version
                        nameJar = artifactName + "-" + artifactVersion + ".jar"
                    }
                }
            }
        }

        stage("Checkout config"){
            steps{
                sh "mkdir config-files"
                dir("config-files"){
                    git credentialsId: 'git-wilmer', url: 'https://github.com/wilmeraguilera/lab-openshift-config.git'
                }
            }

        }

        stage("Build") {
            steps {
                echo "Init Build"
                //Only apply the next instruction if you have the code in a subdirectory
                dir("code-app/backend-users") {
                    sh "mvn -Dmaven.test.skip=true compile"
                }
                echo "End Build"
            }
        }

        stage("Unit Test") {
            steps {
                echo "Init Unit Test"
                dir("code-app/backend-users") {
                    sh "mvn test"
                }
                echo "End Unit Test"
            }
        }


        stage('SonarQube Scan') {
            steps {
                dir("code-app/backend-users") {
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
                //Only apply the next instruction if you have the code in a subdirectory
                dir("code-app/backend-users") {
                    sh "mvn deploy -DskipTests=true -s ./configuration/settings-maven.xml"
                    //-s ./configuration/settings-maven.xml
                }
                echo "End Publish to Nexus"
            }
        }

        stage("Deploy Artifact") {
            steps {
                echo "Deploy Artifact"
            }
        }

        stage("Build Image") {
            steps {
                dir("code-app/backend-users") {
                    echo "Inicia creación image"
                    echo tagImage
                    sh "oc start-build ${params.appName} --from-file=./target/${nameJar} --wait=true -n ${params.namespace_dev}"
                    sh "oc tag ${params.appName}:latest ${params.appName}:${tagImage} -n ${params.namespace_dev}"
                    echo "Termina creación image"
                }
            }
        }

        stage("Deploy DEV") {
            steps {

                echo '${env.WORKSPACE}';

                dir('code-app/backend-users/src/main/resources') {
                    script {
                        //Crear archivo de propiedades dev
                        replaceValuesInFile('${env.WORKSPACE}/config-files/config-dev.properties', 'application-env.properties', 'application.properties')

                    }
                }

                dir("backend-users") {
                    script {
                        //input 'Deploy?'
                        echo "Inicia Deploy"

                        sh "oc delete cm myconfigmap --ignore-not-found=true -n ${params.namespace_dev}"
                        sh "oc create cm myconfigmap --from-file=./src/main/resources/application.properties -n ${params.namespace_dev}"

                        sh "oc set image dc/${params.appName} ${params.appName}=${params.namespace_dev}/${params.appName}:${tagImage} --source=imagestreamtag -n ${params.namespace_dev}"
                        sh "oc rollout latest dc/${params.appName} -n ${params.namespace_dev}"

                        def dc_version = sh(script: "oc get dc/${params.appName} -o=yaml -n ${params.namespace_dev} | grep 'latestVersion'| cut -d':' -f 2", returnStdout: true).trim();
                        echo "Version de DeploymentConfig Actual ${dc_version}"

                        def rc_replicas = sh(returnStdout: true, script: "oc get rc/${params.appName}-${dc_version} -o yaml -n ${params.namespace_dev} |grep -A 5  'status:' |grep 'replicas:' | cut -d ':' -f2").trim()
                        def rc_replicas_ready = sh(returnStdout: true, script: "oc get rc/${params.appName}-${dc_version} -o yaml -n ${params.namespace_dev} |grep -A 5  'status:' |grep 'readyReplicas:' | cut -d ':' -f2").trim()

                        echo "Replicas Deseadas ${rc_replicas} - Replicas Listas ${rc_replicas_ready}"

                        def countIterMax = 20
                        def countInterActual = 0

                        while ((rc_replicas != rc_replicas_ready) && countInterActual <= countIterMax) {
                            sleep 10

                            rc_replicas = sh(returnStdout: true, script: "oc get rc/${params.appName}-${dc_version} -o yaml -n ${params.namespace_dev} |grep -A 5  'status:' |grep 'replicas:' | cut -d ':' -f2").trim()
                            rc_replicas_ready = sh(returnStdout: true, script: "oc get rc/${params.appName}-${dc_version} -o yaml -n ${params.namespace_dev} |grep -A 5  'status:' |grep 'readyReplicas:' | cut -d ':' -f2").trim()

                            echo "Replicas Deseadas ${rc_replicas} - Replicas Listas ${rc_replicas_ready}"

                            countInterActual = countInterActual + 1
                            echo "Iteracion Actual: " + countInterActual
                            if (countInterActual > countIterMax) {
                                echo "Se ha superado el tiempo de espera para el despliegue"
                                echo "Se procede a cancelar el despliegue y a mantener la última versión estable"
                                sh "oc rollout cancel dc/${params.appName}s -n ${params.namespace_dev}"
                                throw new Exception("Se ha superado el tiempo de espera para el despliegue")
                            }
                            echo "Termina Deploy"
                        }
                    }
                }
                echo "Deploy DEV"
            }
        }

        stage("Deploy QA") {
            steps {
                dir('backend-users/src/main/resources') {
                    script {
                        //Crear archivo de propiedades QA
                        replaceValuesInFile('configEnviroment/config-qa.properties', 'application-env.properties', 'application-qa.properties')
                    }
                }
            }
        }
    }
}

/**
 * Metodo encargado de leer una archico de propiedades y reemplazar los valores en en achivo destino.
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

