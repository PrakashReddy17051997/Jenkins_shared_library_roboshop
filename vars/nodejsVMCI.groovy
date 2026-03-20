pipeline {
    agent {
        node {
            label 'agent-1'
        }
    }
    environment {
        packageVersion = ''
        nexusUrl = '172.31.31.230:8081'

    }
    options {
        timeout(time: 1, unit:'HOURS')
        disableConcurrentBuilds()
    }
    parameters{
        booleanParam(name: 'Deploy', defaultValue: false, description: 'Select deploy if the code pass QA')
    }
    stages{
        stage('Get Version'){
            steps{
                script{
                    def appVersion = readJSON file: 'package.json'
                    packageVersion = appVersion.version 
                    echo "application version: $packageVersion"
                }

            }
            }
    
            stage('Install Dependencies'){
                steps{
                    sh """
                        npm install
                    """                

            }
            
            }
            stage('Unit Test'){
                steps{
                    sh """
                        sonar-scanner
                    """
                }
            }
            stage('Build'){
                steps{
                    sh """
                    ls -la
                    zip -q -r catalogue.zip ./* -x ".git" -x ".zip"
                    ls -ltr
                    """
                }
            }
            stage('Publish Artifact') {
                steps {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: "${nexusUrl}",
                        groupId: 'com.roboshop',
                        version: "${packageVersion}",
                        repository: 'catalogue',
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: 'catalogue',
                            classifier: '',
                            file: 'catalogue.zip',
                            type: 'zip']
                        ]
                    )
                }
            }
            stage('Deploy') {
            when {
                expression{
                    params.Deploy == 'true'
                    }
            }
            steps{
                script{
                    def params = [
                    string(name: 'VERSION', value: "$packageVersion"),
                    string(name: 'environment', value: "dev")
                    ]
            
                    build job: "catalogue-deploy", wait: true, parameters: params     
        }
        }
          
    }
    }
    // post build
    post {
        always {
            echo 'We Will always Win again!'
            deleteDir()
        }
        failure {
            echo 'failure occured, inform the concerned teams over slack'
        }
        success {
            echo 'Confragulations on success build'
        }
        
    }
}

