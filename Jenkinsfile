pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        REGISTRY = '172.21.33.225:5000'
        USER_SERVER = '172.21.33.210'
        ADMIN_SERVER = '172.21.33.249'
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
                    sh """
                        ${tool 'sonar-scanner'}/bin/sonar-scanner \
                            -Dsonar.projectKey=sofit-backend \
                            -Dsonar.sources=sofit-user/src/main/java,sofit-admin/src/main/java,sofit-common/src/main/java \
                            -Dsonar.java.binaries=sofit-user/build/classes/java/main,sofit-admin/build/classes/java/main,sofit-common/build/classes/java/main
                    """
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh '''
                    docker build -t $REGISTRY/sofit-user-back:latest -f sofit-user/Dockerfile .
                    docker push $REGISTRY/sofit-user-back:latest

                    docker build -t $REGISTRY/sofit-admin-back:latest -f sofit-admin/Dockerfile .
                    docker push $REGISTRY/sofit-admin-back:latest

                    docker image prune -f
                '''
            }
        }

        stage('Deploy') {
            steps {
                sshagent(['sofit-app-ssh']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no ubuntu@$USER_SERVER "
                            docker pull $REGISTRY/sofit-user-back:latest &&
                            docker-compose -f /home/ubuntu/docker-compose.yml down &&
                            docker-compose -f /home/ubuntu/docker-compose.yml up -d
                        "

                        ssh -o StrictHostKeyChecking=no ubuntu@$ADMIN_SERVER "
                            docker pull $REGISTRY/sofit-admin-back:latest &&
                            docker-compose -f /home/ubuntu/docker-compose.yml down &&
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
