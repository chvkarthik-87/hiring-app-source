
pipeline {
    agent any

    environment {
        APP_VERSION = "1.0.${BUILD_NUMBER}"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Set Dynamic Maven Version') {
            steps {
                sh '''
                    mvn versions:set \
                    -DnewVersion=${APP_VERSION} \
                    -DgenerateBackupPoms=false
                '''
            }
        }

        stage('Maven Build and SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn -B clean verify \
                        org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                        -Dsonar.projectKey=Hiring-App
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Upload Artifact to Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )
                ]) {
                    sh '''
                        cat > settings.xml <<EOF
<settings>
    <servers>
        <server>
            <id>nexus</id>
            <username>${NEXUS_USER}</username>
            <password>${NEXUS_PASS}</password>
        </server>
    </servers>
</settings>
EOF

                        mvn -B deploy -DskipTests -s settings.xml
                    '''
                }
            }
        }

        stage('Trigger Docker Build') {
            steps {
                build job: 'Hiring-App-Docker-Build',
                    parameters: [
                        string(name: 'APP_VERSION', value: "${APP_VERSION}"),
                        string(name: 'IMAGE_TAG', value: "${IMAGE_TAG}")
                    ],
                    wait: true,
                    propagate: true
            }
        }
    }

    post {
        success {
            echo 'CI, SonarQube, Nexus and Docker Trigger Successful'
        }

        failure {
            echo 'CI/CD Pipeline Failed'
        }

        always {
            sh 'rm -f settings.xml'
        }
    }
}
