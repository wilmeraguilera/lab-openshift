pipeline {
    agent any

    tools {
        maven 'M2-3.6.3'
        jdk 'JDK18'
    }

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
                echo "End Unit Test"
            }
        }

        stage("Execute Sonar analysis") {
            steps {
                echo "Execute Sonar analysis"
            }
        }

        stage("Publish to Nexus") {
            steps {
                echo "Publish to Nexus"
            }
        }

        stage("Build Image") {
            steps {
                echo "Build Image"
            }
        }

        stage("Deploy DEV") {
            steps {

                dir('backend-users/src/main/resources'){
                    script {

                        //Crear archivo de propiedades dev
                        replaceValuesInFile('application.properties', 'application-env.properties','application-dev.properties')


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
    echo "Contenido leido: "+textTemplate

    props.each { property ->
        echo property.key
        echo property.value
        textTemplate = textTemplate.replace('${' + property.key + '}', property.value)
    }



    echo "Contenido Reemplazado: "+textTemplate

    text = textTemplate

    writeFile(file: destinationFile, text: text, encoding: "UTF-8")
}

