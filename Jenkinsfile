
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_IMAGE = 'reserve-by-sms-webhook'
        APP_CONTAINER = 'reserve-by-sms-webhook'
        DEPLOY_ENV_FILE = '/opt/reserve-by-sms-webhook/.env'
        COMPOSE_FILE = 'compose.yaml'
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

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu

                    docker compose \
                      -f "$COMPOSE_FILE" \
                      build app
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -eu

                    test -r "$DEPLOY_ENV_FILE" || {
                      echo "Deployment environment file is missing or unreadable"
                      exit 1
                    }

                    docker network inspect reserve-network \
                      >/dev/null 2>&1 || \
                      docker network create reserve-network

                    # 기존 docker run 방식으로 실행한 컨테이너와의
                    # 최초 전환 시 이름 충돌을 방지한다.
                    docker rm -f "$APP_CONTAINER" \
                      >/dev/null 2>&1 || true

                    docker compose \
                      -f "$COMPOSE_FILE" \
                      up -d --force-recreate

                    sleep 10

                    if [ "$(docker inspect \
                      --format '{{.State.Running}}' "$APP_CONTAINER")" != "true" ]; then
                        docker logs --tail=100 "$APP_CONTAINER" || true
                        exit 1
                    fi

                    echo "Container is running"
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
