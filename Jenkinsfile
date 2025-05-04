pipeline {
    agent any

    environment {
        IMAGE_TAG = "${GIT_COMMIT}" // Tagging with the commit ID
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm  // Checkout source code from the repository
            }
        }

        stage('Detect Changed Services') {
            steps {
                script {
                    // Get the list of files changed between HEAD~1 and HEAD
                    def output = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                    def files = output.tokenize('\n')

                    // Filter for services that have changes (directories starting with spring-petclinic-)
                    changedServices = files
                        .findAll { it ==~ /^spring-petclinic-.*/ } // Match directories with spring-petclinic- prefix
                        .collect { it.split('/')[0].replace("spring-petclinic-", "") }  // Get service names after the prefix
                        .unique()

                    // If no services have changed, terminate the pipeline
                    if (changedServices.isEmpty()) {
                        echo "✅ No services have changed, terminating pipeline."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    // Display the changed services
                    echo "🛠 Changed services: ${changedServices}"
                }
            }
        }

        stage('Build & Push Docker Images') {
            when {
                expression { return changedServices && changedServices.size() > 0 }
            }
            steps {
                script {
                    // Using credentials to log in to Docker Hub
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'DOCKER_HUB_USER',
                        passwordVariable: 'DOCKER_HUB_PASS'
                    )]) {
                        docker.withRegistry('', 'dockerhub-credentials') {
                            for (svc in changedServices) {
                                // Construct the full image name with your Docker Hub username
                                def imageName = "${DOCKER_HUB_USER}/spring-petclinic-${svc}"
                                def fullTag   = "${imageName}:${IMAGE_TAG}"

                                echo "🚧 Building image ${fullTag}..."
                                // Build the Docker image with the specified tag
                                def img = docker.build(fullTag, "--file spring-petclinic-${svc}/Dockerfile spring-petclinic-${svc}")

                                echo "🚀 Pushing image ${fullTag}..."
                                // Push the built image to Docker Hub
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
            echo "🎉 Build and Push successful!"
        }
        failure {
            echo "❌ An error occurred in the pipeline."
        }
    }
}
