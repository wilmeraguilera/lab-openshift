pipeline {
    agent any

    tools {
        maven 'M2-3.6.3'
        jdk 'JDK18'
    }

    /**parameters(
            [
                    string(name: 'namespace_dev', defaultValue: 'dev-admin-users', description:'Nombre del proyecto en Openshift para DEV'),
                    string(name: 'namespace_qa', defaultValue: 'qa-admin-users', description:'Nombre del proyecto en Openshift para QA'),
                    string(name: 'namespace_prod', defaultValue: 'prod-admin-users', description:'Nombre del proyecto en Openshift para PROD'),
                    string(name: 'appName', defaultValue: 'api-users', description:'Nombre de la aplicación')
            ]
    )*/

    stages {
        stage("Checkout Source Code") {
            steps {
                echo "Checkout Source Code"
                checkout scm
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
                    //sh "mvn test"
                }
                echo "End Unit Test"
            }
        }



        stage('SonarQube Scan') {
            steps{
                dir("backend-users"){
                    echo "Init Running Code Analysis"
                    withSonarQubeEnv('sonar') {
                        sh "mvn sonar:sonar " +
                                "-Dsonar.java.coveragePlugin=jacoco -Dsonar.junit.reportsPath=target/surefire-reports  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml "

                    }

                    echo "End Running Code Analysis"
                }
            }
        }

        stage("Quality Gate") {
            steps {
                sleep(10)
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage("Publish to Nexus") {
            steps {
                echo "Init Publish to Nexus"
                //Only apply the next instruction if you have the code in a subdirectory
                dir("backend-users") {
                    sh "mvn install"
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

        stage("Deploy DEV") {
            steps {

                dir('backend-users/src/main/resources'){
                    script {

                        //Crear archivo de propiedades dev
                        replaceValuesInFile('configEnviroment/config-dev.properties', 'application-env.properties','application-dev.properties')

                    }
                }
                echo "Deploy DEV"
            }
        }
    }
}

def replaceValuesInFile(valuesPropertiesFile, templateFile, destinationFile){
    def props = readProperties  file: valuesPropertiesFile

    def textTemplate = readFile templateFile
    echo "Contenido leido del template: "+textTemplate

    props.each { property ->
        echo property.key
        echo property.value
        textTemplate = textTemplate.replace('${' + property.key + '}', property.value)
    }

    echo "Contenido Reemplazado: "+textTemplate

    finalText = textTemplate
    writeFile(file: destinationFile, text: finalText, encoding: "UTF-8")
}

