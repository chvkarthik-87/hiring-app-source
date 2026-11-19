
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
                        "$NEXUS_URL/repository/maven-releases/com/hiring/hiring-app/1.0.1/hiring-app-1.0.1.jar" \
                        -o target/hiring-app-1.0.1.jar

                        ls -lh target/hiring-app-1.0.1.jar
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

        stage('Update GitOps Repository') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'github-k8s-creds',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_TOKEN'
                    )
                ]) {
                    sh '''
                        set +x

                        cat > askpass.sh <<'EOF'
#!/bin/sh
case "$1" in
    *Username*) printf '%s' "$GIT_USER" ;;
    *Password*) printf '%s' "$GIT_TOKEN" ;;
esac
EOF

                        chmod 700 askpass.sh

                        export GIT_ASKPASS="$WORKSPACE/askpass.sh"
                        export GIT_TERMINAL_PROMPT=0

                        git clone \
                        https://github.com/chvkarthik-87/hiring-app-k8s.git \
                        gitops

                        cd gitops

                        sed -i -E \
                        "s#(image:[[:space:]]*chvkarthik/hiring-app:).*#\\1${BUILD_NUMBER}#" \
                        k8s/deployment.yaml

                        git config user.name "Jenkins"
                        git config user.email "jenkins@users.noreply.github.com"

                        git add k8s/deployment.yaml

                        if git diff --cached --quiet; then
                            echo "No GitOps changes to commit"
                        else
                            git commit -m "Update hiring-app image to ${BUILD_NUMBER}"
                            git push origin main
                        fi

                        cd "$WORKSPACE"
                        rm -f askpass.sh
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Docker Image Build, Push and GitOps Update Successful'
        }

        failure {
            echo 'Docker or GitOps Pipeline Failed'
        }
    }
}
