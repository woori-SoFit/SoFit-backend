pipeline {
    agent any

    environment {
        REGISTRY = '172.21.33.225:5000'
        APP_SERVER = '172.21.33.238'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './gradlew :sofit-user:bootJar :sofit-admin:bootJar -x test --rerun-tasks'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh './gradlew sonar --rerun-tasks'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh '''
                    docker build --no-cache -t $REGISTRY/sofit-user-back:latest -f sofit-user/Dockerfile .
                    docker push $REGISTRY/sofit-user-back:latest

                    docker build --no-cache -t $REGISTRY/sofit-admin-back:latest -f sofit-admin/Dockerfile .
                    docker push $REGISTRY/sofit-admin-back:latest
                '''
            }
        }

        stage('Deploy') {
            steps {
                sshagent(['sofit-app-ssh']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no ubuntu@$APP_SERVER "
                            docker pull $REGISTRY/sofit-user-back:latest &&
                            docker pull $REGISTRY/sofit-admin-back:latest &&
                            docker-compose -f /home/ubuntu/docker-compose.yml up -d
                        "
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '배포 성공'
        }
        failure {
            echo '배포 실패'
        }
    }
}