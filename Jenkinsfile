pipeline {
    agent any

    environment {
        IMAGE_TAG = "${GIT_COMMIT}" // Gắn tag bằng commit ID
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm  // Checkout mã nguồn từ repo
            }
        }

        stage('Detect Changed Services') {
            steps {
                script {
                    // Lấy danh sách các file thay đổi giữa HEAD~1 và HEAD
                    def output = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                    def files = output.tokenize('\n')

                    // Lọc ra các dịch vụ có thay đổi (danh sách các thư mục bắt đầu bằng spring-petclinic-)
                    changedServices = files
                        .findAll { it ==~ /^spring-petclinic-.*/ } // Các file trong thư mục có tiền tố spring-petclinic-
                        .collect { it.split('/')[0].replace("spring-petclinic-", "") }  // Lấy tên service sau tiền tố
                        .unique()

                    // Nếu không có dịch vụ nào thay đổi, kết thúc pipeline
                    if (changedServices.isEmpty()) {
                        echo "✅ Không có service nào thay đổi, kết thúc pipeline."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    // Hiển thị các dịch vụ đã thay đổi
                    echo "🛠 Dịch vụ thay đổi: ${changedServices}"
                }
            }
        }

        stage('Build & Push Docker Images') {
            when {
                expression { return changedServices && changedServices.size() > 0 }  // Kiểm tra nếu có dịch vụ thay đổi
            }
            steps {
                script {
                    // Đăng nhập vào Docker Hub với credentials
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials', // Đảm bảo ID này đúng trong Jenkins
                        usernameVariable: 'DOCKER_HUB_USER',
                        passwordVariable: 'DOCKER_HUB_PASS'
                    )]) {
                        // Lặp qua từng service thay đổi và thực hiện build + push Docker image
                        for (svc in changedServices) {
                            def image = "${DOCKER_HUB_USER}/spring-petclinic-${svc}:${IMAGE_TAG}"  // Tạo tên image với tiền tố spring-petclinic-

                            echo "🚧 Đang xử lý ${svc}..."

                            sh """
                                cd spring-petclinic-${svc}  
                                mvn clean package -DskipTests  
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
