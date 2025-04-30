pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Branch to build')
    }

    environment {
        DOCKER_HUB_USERNAME = 'thainhat'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "${params.BRANCH_NAME}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    userRemoteConfigs: [[
                        credentialsId: 'github-credentials',
                        url: 'https://github.com/thainhat04/spring-petclinic-microservices.git'
                    ]]
                ])
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    env.COMMIT_ID = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "🧠 Commit ID: ${env.COMMIT_ID}"

                    def changedFiles = sh(script: 'git diff-tree --no-commit-id --name-only -r HEAD', returnStdout: true).trim()

                    def allServices = [
                        'spring-petclinic-admin-server',
                        'spring-petclinic-api-gateway',
                        'spring-petclinic-customers-service',
                        'spring-petclinic-discovery-server',
                        'spring-petclinic-vets-service',
                        'spring-petclinic-visits-service',
                        'spring-petclinic-genai-service',
                        'spring-petclinic-config-server'
                    ]

                    def changedServices = []
                    for (service in allServices) {
                        if (changedFiles.contains(service)) {
                            changedServices << service
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "❗Không phát hiện thay đổi, sẽ build tất cả service."
                        changedServices = allServices
                    } else {
                        echo "🔍 Các service thay đổi: ${changedServices.join(', ')}"
                    }

                    env.CHANGED_SERVICES = changedServices.join(" ")
                }
            }
        }

        stage('Build JAR') {
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(" ")
                    for (service in services) {
                        dir(service.trim()) {
                            echo "🔨 Building JAR for ${service}"
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    script {
                        def services = env.CHANGED_SERVICES.split(" ")
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'

                        for (service in services) {
                            def image = "${DOCKER_USER}/${service.trim()}"
                            dir(service.trim()) {
                                echo "🐳 Build & push image: ${image}:${env.COMMIT_ID}"
                                sh """
                                    docker build -t ${image}:${env.COMMIT_ID} .
                                    docker push ${image}:${env.COMMIT_ID}
                                """

                                if (params.BRANCH_NAME == 'main') {
                                    sh """
                                        docker tag ${image}:${env.COMMIT_ID} ${image}:latest
                                        docker push ${image}:latest
                                    """
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Cleaning workspace & logout Docker...'
            sh 'docker logout || true'
            cleanWs()
        }
    }
}
