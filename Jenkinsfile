pipeline {
    agent any
    options { disableConcurrentBuilds() }
    triggers { githubPush() }
    environment {
        REGISTRY = '172.21.33.225:5000'
        USER_SERVER = '172.21.33.210'
        ADMIN_SERVER = '172.21.33.249'
    }
    stages {
        stage('Cleanup') {
            steps {
                sh 'docker image prune -a -f --filter until=72h || true'
            }
        }
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Build Common') {
            steps {
                sh './gradlew :sofit-common:jar -x test --rerun-tasks'
            }
        }
        stage('Build') {
            parallel {
                stage('sofit-user') {
                    when {
                        anyOf {
                            changeset 'sofit-user/**'
                            changeset 'sofit-common/**'
                            triggeredBy 'UserIdCause'
                        }
                    }
                    steps { sh './gradlew :sofit-user:bootJar -x test' }
                }
                stage('sofit-admin') {
                    when {
                        anyOf {
                            changeset 'sofit-admin/**'
                            changeset 'sofit-common/**'
                            triggeredBy 'UserIdCause'
                        }
                    }
                    steps { sh './gradlew :sofit-admin:bootJar -x test' }
                }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh """${tool 'sonar-scanner'}/bin/sonar-scanner \
                        -Dsonar.projectKey=sofit-backend \
                        -Dsonar.sources=sofit-user/src/main/java,sofit-admin/src/main/java,sofit-common/src/main/java \
                        -Dsonar.java.binaries=sofit-user/build/classes/java/main,sofit-admin/build/classes/java/main,sofit-common/build/classes/java/main"""
                }
            }
        }
        stage('Docker Build & Push') {
            parallel {
                stage('sofit-user-back') {
                    when {
                        anyOf {
                            changeset 'sofit-user/**'
                            changeset 'sofit-common/**'
                            triggeredBy 'UserIdCause'
                        }
                    }
                    steps { sh 'docker build -t $REGISTRY/sofit-user-back:latest -f sofit-user/Dockerfile . && docker push $REGISTRY/sofit-user-back:latest' }
                }
                stage('sofit-admin-back') {
                    when {
                        anyOf {
                            changeset 'sofit-admin/**'
                            changeset 'sofit-common/**'
                            triggeredBy 'UserIdCause'
                        }
                    }
                    steps { sh 'docker build -t $REGISTRY/sofit-admin-back:latest -f sofit-admin/Dockerfile . && docker push $REGISTRY/sofit-admin-back:latest' }
                }
            }
        }
        stage('Deploy') {
            parallel {
                stage('sofit-user-back') {
                    when {
                        anyOf {
                            changeset 'sofit-user/**'
                            changeset 'sofit-common/**'
                            triggeredBy 'UserIdCause'
                        }
                    }
                    steps {
                        sshagent(['sofit-app-ssh']) {
                            sh 'ssh -o StrictHostKeyChecking=no ubuntu@$USER_SERVER "docker pull $REGISTRY/sofit-user-back:latest && docker compose -f /home/ubuntu/docker-compose.yml up -d --force-recreate sofit-user-back"'
                        }
                    }
                }
                stage('sofit-admin-back') {
                    when {
                        anyOf {
                            changeset 'sofit-admin/**'
                            changeset 'sofit-common/**'
                            triggeredBy 'UserIdCause'
                        }
                    }
                    steps {
                        sshagent(['sofit-app-ssh']) {
                            sh 'ssh -o StrictHostKeyChecking=no ubuntu@$ADMIN_SERVER "docker pull $REGISTRY/sofit-admin-back:latest && docker compose -f /home/ubuntu/docker-compose.yml up -d --force-recreate sofit-admin-back"'
                        }
                    }
                }
            }
        }
    }
    post {
        success { echo '배포 성공' }
        failure { echo '배포 실패' }
    }
}
