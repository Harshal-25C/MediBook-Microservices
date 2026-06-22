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

        stage('Prepare ENV') {
            steps {
                sh 'cp /var/jenkins_home/.env .env'
            }
        }

        stage('Build JARs') {
            steps {
                sh 'mvn clean install -DskipTests'
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
                        passwordVariable: 'PASS'
                    )
                ]) {
                    sh 'echo $PASS | docker login -u $USER --password-stdin'
                }
            }
        }

        stage('Tag & Push Images') {
            steps {
                sh '''
                    services="eureka-server api-gateway auth-service admin-service provider-service schedule-service appointment-service payment-service review-service notification-service record-service"
                    for svc in $services; do
                        docker tag medibook-cicd-${svc}:latest ${DOCKERHUB}/${svc}:${IMAGE_TAG}
                        docker push ${DOCKERHUB}/${svc}:${IMAGE_TAG}
                    done
                '''
            }
        }

        stage('Deploy EC2') {
            steps {
                sshagent(['ec2-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no ubuntu@13.60.223.73 \
                        "cd ~/MediBook-Microservices && \
                        git pull origin main && \
                        cp ~/.env .env && \
                        docker-compose pull && \
                        docker-compose up -d --remove-orphans"
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check logs above.'
        }
    }
}