pipeline {
    agent any
    
    environment {
        // Khai báo các biến môi trường
        DOCKER_HUB_CREDS = credentials('dockerhub-credentials')
        COMMIT_ID = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        BRANCH_NAME = env.BRANCH_NAME
        DOCKER_HUB_USERNAME = 'thainhat' // Thay thế bằng username Docker Hub của bạn
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Checkout code từ Git repository
                checkout scm
            }
        }
        
        stage('Build Application') {
            steps {
                // Build ứng dụng với Maven
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    // Lấy tên service từ thư mục hiện tại
                    def serviceName = sh(script: 'basename `pwd`', returnStdout: true).trim()
                    
                    // Build Docker image với tag là commit ID
                    sh "docker build -t ${DOCKER_HUB_USERNAME}/${serviceName}:${COMMIT_ID} ."
                    
                    // Build thêm image với tag là tên branch cho dễ tham chiếu
                    sh "docker build -t ${DOCKER_HUB_USERNAME}/${serviceName}:${BRANCH_NAME}-latest ."
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    def serviceName = sh(script: 'basename `pwd`', returnStdout: true).trim()
                    
                    // Đăng nhập vào Docker Hub
                    sh "echo ${DOCKER_HUB_CREDS_PSW} | docker login -u ${DOCKER_HUB_CREDS_USR} --password-stdin"
                    
                    // Push Docker images lên Docker Hub
                    sh "docker push ${DOCKER_HUB_USERNAME}/${serviceName}:${COMMIT_ID}"
                    sh "docker push ${DOCKER_HUB_USERNAME}/${serviceName}:${BRANCH_NAME}-latest"
                }
            }
        }
        
        stage('Clean Up') {
            steps {
                // Xóa các Docker images cục bộ để tiết kiệm không gian
                script {
                    def serviceName = sh(script: 'basename `pwd`', returnStdout: true).trim()
                    
                    sh "docker rmi ${DOCKER_HUB_USERNAME}/${serviceName}:${COMMIT_ID}"
                    sh "docker rmi ${DOCKER_HUB_USERNAME}/${serviceName}:${BRANCH_NAME}-latest"
                }
            }
        }
    }
    
    post {
        always {
            // Đăng xuất khỏi Docker Hub
            sh "docker logout"
        }
    }
}