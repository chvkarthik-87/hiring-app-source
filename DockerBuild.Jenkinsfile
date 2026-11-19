
pipeline {
    agent any

    parameters {
        string(
            name: 'APP_VERSION',
            defaultValue: '',
            description: 'Maven artifact version from Hiring-App-CI'
        )
        string(
            name: 'IMAGE_TAG',
            defaultValue: '',
            description: 'Docker image tag from Hiring-App-CI'
        )
    }

    environment {
        DOCKER_IMAGE = 'chvkarthik/hiring-app'
        NEXUS_URL = 'http://172.31.20.79:8081'
        GITOPS_REPO = 'https://github.com/chvkarthik-87/hiring-app-k8s.git'
    }

    stages {

        stage('Validate Parameters') {
            steps {
                script {
                    if (!params.APP_VERSION?.trim() ||
                        !params.IMAGE_TAG?.trim()) {
                        error('APP_VERSION and IMAGE_TAG must be supplied by Hiring-App-CI')
                    }

                    if (!(params.APP_VERSION ==~ /^[0-9]+(\.[0-9A-Za-z-]+)*$/) ||
                        !(params.IMAGE_TAG ==~ /^[A-Za-z0-9_.-]+$/)) {
                        error('Invalid APP_VERSION or IMAGE_TAG format')
                    }
                }

                echo "Maven Version: ${params.APP_VERSION}"
                echo "Docker Image Tag: ${params.IMAGE_TAG}"
            }
        }

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
                        set -e

                        mkdir -p target

                        curl --fail --show-error --location \
                        --retry 3 \
                        -u "$NEXUS_USER:$NEXUS_PASS" \
                        "$NEXUS_URL/repository/maven-releases/com/hiring/hiring-app/${APP_VERSION}/hiring-app-${APP_VERSION}.jar" \
                        -o target/app.jar

                        test -s target/app.jar

                        echo "Downloaded artifact successfully"
                        ls -lh target/app.jar
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -e

                    docker build \
                    -t ${DOCKER_IMAGE}:${IMAGE_TAG} .

                    docker images ${DOCKER_IMAGE}:${IMAGE_TAG}
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
                        set +x
                        set -e

                        echo "$DOCKER_PASS" | docker login \
                        -u "$DOCKER_USER" --password-stdin

                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}

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
                        set -e

                        ASKPASS="$WORKSPACE/askpass.sh"

                        cleanup() {
                            rm -f "$ASKPASS"
                        }

                        trap cleanup EXIT

                        cat > "$ASKPASS" <<'EOF'
#!/bin/sh
case "$1" in
    *Username*) printf '%s' "$GIT_USER" ;;
    *Password*) printf '%s' "$GIT_TOKEN" ;;
esac
EOF

                        chmod 700 "$ASKPASS"

                        export GIT_ASKPASS="$ASKPASS"
                        export GIT_TERMINAL_PROMPT=0

                        rm -rf "$WORKSPACE/gitops"

                        git clone \
                        "$GITOPS_REPO" \
                        "$WORKSPACE/gitops"

                        cd "$WORKSPACE/gitops"

                        test -f k8s/deployment.yaml

                        sed -i -E \
                        "s#(image:[[:space:]]*chvkarthik/hiring-app:).*#\\1${IMAGE_TAG}#" \
                        k8s/deployment.yaml

                        grep -F "image: chvkarthik/hiring-app:${IMAGE_TAG}" \
                        k8s/deployment.yaml

                        git config user.name "Jenkins"
                        git config user.email "jenkins@users.noreply.github.com"

                        git add k8s/deployment.yaml

                        if git diff --cached --quiet; then
                            echo "GitOps manifest already contains this image tag"
                        else
                            git commit -m "Update hiring-app image to ${IMAGE_TAG}"
                            git push origin main
                        fi
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

        always {
            sh '''
                rm -f "$WORKSPACE/askpass.sh"
            '''
        }
    }
}
