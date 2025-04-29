pipeline {
    agent any
    
    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Branch to build')
    }
    
    environment {
        DOCKER_HUB_CREDS = credentials('dockerhub-credentials')
        DOCKER_HUB_USERNAME = 'thainhat' // Replace with actual username
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
                    // Get commit ID
                    env.COMMIT_ID = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "Building for commit: ${env.COMMIT_ID}"
                    
                    // Detect changed services
                    def changedFiles = sh(script: 'git diff-tree --no-commit-id --name-only -r HEAD', returnStdout: true).trim()
                    
                    // Define services to check
                    def services = [
                        'petclinic-admin-server',
                        'petclinic-api-gateway',
                        'petclinic-customers-service',
                        'petclinic-discovery-server',
                        'petclinic-vets-service',
                        'petclinic-visits-service',
                        'petclinic-genai-service'
                    ]
                    
                    // Determine which services changed
                    env.CHANGED_SERVICES = ""
                    for (service in services) {
                        if (changedFiles.contains(service)) {
                            env.CHANGED_SERVICES += "${service} "
                        }
                    }
                    
                    if (env.CHANGED_SERVICES == "") {
                        echo "No specific service detected in changes. Will build all services."
                        env.CHANGED_SERVICES = services.join(" ")
                    } else {
                        echo "Detected changes in services: ${env.CHANGED_SERVICES}"
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
                    sh 'echo $DOCKER_HUB_CREDS_PSW | docker login -u $DOCKER_HUB_CREDS_USR --password-stdin'
                    
                    def servicesList = env.CHANGED_SERVICES.split(" ")
                    for (service in servicesList) {
                        if (service.trim()) {
                            echo "Building and pushing Docker image for ${service}"
                            
                            dir(service) {
                                // Build the Docker image with commit ID tag
                                sh """
                                docker build -t ${DOCKER_HUB_USERNAME}/${service}:${COMMIT_ID} .
                                docker push ${DOCKER_HUB_USERNAME}/${service}:${COMMIT_ID}
                                
                                if [ "${params.BRANCH_NAME}" = "main" ]; then
                                    docker tag ${DOCKER_HUB_USERNAME}/${service}:${COMMIT_ID} ${DOCKER_HUB_USERNAME}/${service}:latest
                                    docker push ${DOCKER_HUB_USERNAME}/${service}:latest
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
            sh 'docker logout'
            cleanWs()
        }
    }
}   