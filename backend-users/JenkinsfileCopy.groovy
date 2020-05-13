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
                        //leer propiedades
                        def props = readProperties  file: 'application.properties'

                        def textTemplate = readFile "application-env.properties"
                        echo "Contenido leido: "+textTemplate

                        props.each { property ->
                            echo property.key
                            echo property.value
                            textTemplate = textTemplate.replace('#{' + property.key + '}', property.value)
                        }



                        echo "Contenido Reemplazado: "+textTemplate

                        text = textTemplate

                        writeFile(file: "application-dev.properties", text: text, encoding: "UTF-8")

                        sh (script : 'cat application-dev.properties', returnStdout: true)
                  //replaceValuesInFile('application.properties','application-dev.properties','application-env.properties')
                    }
                }
                echo "Deploy DEV"
            }
        }
    }
}
