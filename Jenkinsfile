pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        DEPLOY_ENV_FILE = '/opt/reserve-by-sms-webhook/.env'
        COMPOSE_PROJECT_NAME = 'reserve-by-sms-webhook'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh '''
                    chmod +x ./gradlew
                    ./gradlew clean test --no-daemon
                '''
            }
        }

        stage('Validate Docker Compose') {
            steps {
                sh '''
                    set -eu

                    test -r "$DEPLOY_ENV_FILE" || {
                        echo "Deployment environment file is missing or unreadable"
                        exit 1
                    }

                    test -f compose.yaml || {
                        echo "compose.yaml not found in workspace"
                        exit 1
                    }

                    docker compose \
                        --env-file "$DEPLOY_ENV_FILE" \
                        config -q
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -eu

                    docker compose \
                        --env-file "$DEPLOY_ENV_FILE" \
                        up -d --build --remove-orphans

                    sleep 10

                    if [ "$(docker inspect \
                        --format '{{.State.Running}}' \
                        reserve-by-sms-webhook)" != "true" ]; then
                        docker compose \
                            --env-file "$DEPLOY_ENV_FILE" \
                            logs --tail=100 app || true
                        exit 1
                    fi

                    echo "Application container is running"
                '''
            }
        }
    }

    post {
        success {
            echo 'Build and deployment succeeded.'
        }

        failure {
            echo 'Build or deployment failed. Check the stage logs.'
        }
    }
}