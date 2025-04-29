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
                        url: "https://github.com/thainhat04/spring-petclinic-microservices.git"
                    ]]
                ])
            }
        }

        stage('Determine Changes') {
            steps {
                script {
                    env.COMMIT_ID = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "Building for commit: ${env.COMMIT_ID}"

                    def changedFiles = sh(script: 'git diff-tree --no-commit-id --name-only -r HEAD', returnStdout: true).trim()

                    def services = [
                        'spring-petclinic-admin-server',
                        'spring-petclinic-api-gateway',
                        'spring-petclinic-customers-service',
                        'spring-petclinic-discovery-server',
                        'spring-petclinic-vets-service',
                        'spring-petclinic-visits-service',
                        'spring-petclinic-genai-service'
                    ]

                    def serviceMap = [
                        'spring-petclinic-admin-server': 'petclinic-admin-server',
                        'spring-petclinic-api-gateway': 'petclinic-api-gateway',
                        'spring-petclinic-customers-service': 'petclinic-customers-service',
                        'spring-petclinic-discovery-server': 'petclinic-discovery-server',
                        'spring-petclinic-vets-service': 'petclinic-vets-service',
                        'spring-petclinic-visits-service': 'petclinic-visits-service',
                        'spring-petclinic-genai-service': 'petclinic-genai-service'
                    ]

                    env.SERVICE_MAP = groovy.json.JsonOutput.toJson(serviceMap)

                    env.CHANGED_SERVICES = ""
                    for (service in services) {
                        if (changedFiles.contains(service)) {
                            env.CHANGED_SERVICES += "${service} "
                        }
                    }

                    if (env.CHANGED_SERVICES.trim() == "") {
                        echo "Không phát hiện thay đổi cụ thể. Sẽ build tất cả services."
                        env.CHANGED_SERVICES = services.join(" ")
                    } else {
                        echo "Detected changes in: ${env.CHANGED_SERVICES}"
                    }
                }
            }
        }

        stage('Build Services') {
            steps {
                script {
                    def servicesList = env.CHANGED_SERVICES.split(" ")
                    for (service in servicesList) {
                        if (service.trim()) {
                            dir(service) {
                                echo "Building ${service}"
                                sh 'mvn clean package -DskipTests'
                            }
                        }
                    }
                }
            }
        }

       stage('Build & Push Docker Images') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', 
                                                usernameVariable: 'DOCKER_USER', 
                                                passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        def serviceMap = readJSON text: env.SERVICE_MAP
                        def servicesList = env.CHANGED_SERVICES.split(" ")

                        // Perform Docker login
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'

                        for (service in servicesList) {
                            if (service.trim()) {
                                def imageName = "${DOCKER_USER}/${serviceMap[service]}"
                                dir(service) {
                                    echo "📦 Building & pushing Docker image: ${imageName}:${env.COMMIT_ID}"
                                    sh """
                                        docker build -t ${imageName}:${env.COMMIT_ID} .
                                        docker push ${imageName}:${env.COMMIT_ID}
                                    """
                                    if (params.BRANCH_NAME == 'main') {
                                        sh """
                                            docker tag ${imageName}:${env.COMMIT_ID} ${imageName}:latest
                                            docker push ${imageName}:latest
                                        """
                                    }
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
            echo '🧹 Cleaning up Docker & workspace...'
            sh 'docker logout || true'
            cleanWs()
        }
    }
}
