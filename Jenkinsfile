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
                expression { return changedServices && changedServices.size() > 0 }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'DOCKER_HUB_USER',
                        passwordVariable: 'DOCKER_HUB_PASS'
                    )]) {
                        docker.withRegistry('', 'dockerhub-credentials') {
                            for (svc in changedServices) {
                                // Tạo đầy đủ tên image với namespace Docker Hub
                                def imageName = "${DOCKER_HUB_USER}/spring-petclinic-${svc}"
                                def fullTag   = "${imageName}:${IMAGE_TAG}"

                                echo "🚧 Building image ${fullTag}..."
                                // build image với đầy đủ tên (thay đổi tương tự docker tag + push)
                                def img = docker.build(fullTag, "--file spring-petclinic-${svc}/Dockerfile spring-petclinic-${svc}")

                                echo "🚀 Pushing image ${fullTag}..."
                                img.push()
                            }
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
