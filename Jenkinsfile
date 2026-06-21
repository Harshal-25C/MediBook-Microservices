pipeline {
    agent any
    environment {
        DOCKERHUB = "httpsharsh"
        IMAGE_TAG = "latest"
    }
    stages {
        stage('Clone') {
            steps {
                git branch: 'main',
                url: 'https://github.com/Harshal-25C/MediBook-Microservices.git'
            }
        }
        stage('Build Docker Images') {
            steps {
                sh 'docker-compose build'
            }
        }
        stage('Docker Login') {
            steps {
                withCredentials([
                    usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS')
                ]) {
                    sh '''
                    echo $PASS | docker login -u $USER --password-stdin
                    '''
                }
            }
        }
        stage('Push Images') {
            steps {
                sh 'docker-compose push'
            }
        }
        stage('Deploy EC2') {
            steps {
                sshagent(['ec2-key']) {
                    sh '''
                    ssh -o StrictHostKeyChecking=no ubuntu@YOUR_EC2_IP << EOF
                    cd ~/MediBook-Microservices
                    docker-compose pull
                    docker-compose up -d
                    EOF
                    '''
                }
            }
        }
    }
}
