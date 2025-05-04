pipeline {
    agent any

    environment {
        DOCKER_HUB_USER = 'thainhat'     // 👉 Thay bằng Docker Hub của bạn
        DOCKER_HUB_PASS = credentials('dockerhub-credentials') // 👉 Tên biến secret text đã lưu trong Jenkins
        IMAGE_TAG = "${GIT_COMMIT}" // Gắn tag bằng commit ID
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Changed Services') {
            steps {
                script {
                    def output = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                    def files = output.tokenize('\n')

                    changedServices = files
                        .findAll { it ==~ /^.+-service\/.*/ }    // Chỉ lấy thư mục có tên *-service/*
                        .collect { it.split('/')[0] }
                        .unique()

                    if (changedServices.isEmpty()) {
                        echo "✅ Không có service nào thay đổi, kết thúc pipeline."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    echo "🛠 Service thay đổi: ${changedServices}"
                }
            }
        }

        stage('Build & Push Docker Images') {
            when {
                expression { return changedServices && changedServices.size() > 0 }
            }
            steps {
                script {
                    for (svc in changedServices) {
                        def image = "${DOCKER_HUB_USER}/${svc}:${IMAGE_TAG}"
                        echo "🚧 Đang xử lý ${svc}..."

                        sh """
                            cd ${svc}
                            ./mvnw clean package -DskipTests
                            docker build -t ${image} .
                            echo "${DOCKER_HUB_PASS}" | docker login -u "${DOCKER_HUB_USER}" --password-stdin
                            docker push ${image}
                        """

                        echo "✅ Đã push Docker image: ${image}"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "🎉 Build và Push thành công!"
        }
        failure {
            echo "❌ Có lỗi xảy ra trong pipeline."
        }
    }
}
