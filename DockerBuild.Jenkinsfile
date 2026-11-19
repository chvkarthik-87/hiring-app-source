
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'chvkarthik/hiring-app'
        NEXUS_URL = 'http://172.31.20.79:8081'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Download JAR from Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )
                ]) {
                    sh '''
                        mkdir -p target

                        curl -f -L \
                        -u "$NEXUS_USER:$NEXUS_PASS" \
                        "$NEXUS_URL/repository/maven-releases/com/hiring/hiring-app/1.0.0/hiring-app-1.0.0.jar" \
                        -o target/hiring-app-1.0.0.jar

                        ls -lh target/hiring-app-1.0.0.jar
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build \
                    -t ${DOCKER_IMAGE}:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Docker Hub Login and Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login \
                        -u "$DOCKER_USER" --password-stdin

                        docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}

                        docker logout
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Docker Image Build and Push Successful'
        }

        failure {
            echo 'Docker Image Build or Push Failed'
        }
    }
}
