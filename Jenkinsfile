pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Branch to build')
    }

    environment {
        DOCKER_HUB_CREDS = credentials('dockerhub-creds')
        DOCKER_HUB_USERNAME = 'thainhat' // Đổi thành username Docker Hub của bạn
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "${params.BRANCH_NAME}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
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
                    // Lấy commit ID
                    env.COMMIT_ID = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "Building for commit: ${env.COMMIT_ID}"

                    // Lấy danh sách file thay đổi
                    def changedFiles = sh(script: 'git diff-tree --no-commit-id --name-only -r HEAD', returnStdout: true).trim()

                    // Danh sách thư mục dịch vụ thực tế (trong Git)
                    def services = [
                        'spring-petclinic-admin-server',
                        'spring-petclinic-api-gateway',
                        'spring-petclinic-customers-service',
                        'spring-petclinic-discovery-server',
                        'spring-petclinic-vets-service',
                        'spring-petclinic-visits-service',
                        'spring-petclinic-genai-service'
                    ]

                    // Map ánh xạ tên thư mục → tên Docker image
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

                    // Xác định các service thay đổi
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
                                sh './mvnw clean package -DskipTests'
                            }
                        }
                    }
                }
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                script {
                    def serviceMap = readJSON text: env.SERVICE_MAP
                    def servicesList = env.CHANGED_SERVICES.split(" ")

                    // Đăng nhập Docker Hub
                    sh 'echo $DOCKER_HUB_CREDS_PSW | docker login -u $DOCKER_HUB_CREDS_USR --password-stdin'

                    for (service in servicesList) {
                        if (service.trim()) {
                            def imageName = "${DOCKER_HUB_USERNAME}/${serviceMap[service]}"
                            dir(service) {
                                echo "Building and pushing Docker image: ${imageName}"

                                sh """
                                docker build -t ${imageName}:${COMMIT_ID} .
                                docker push ${imageName}:${COMMIT_ID}

                                if [ "${params.BRANCH_NAME}" = "main" ]; then
                                    docker tag ${imageName}:${COMMIT_ID} ${imageName}:latest
                                    docker push ${imageName}:latest
                                fi
                                """
                            }
                        }
                    }
                }
            }
        }
    }

    post {
    always {
        script {
            sh 'docker logout || true'
            cleanWs()
        }
    }
}

}
