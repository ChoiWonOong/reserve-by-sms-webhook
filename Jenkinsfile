pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_IMAGE = 'reserve-by-sms-webhook'
        APP_CONTAINER = 'reserve-by-sms-webhook'
        APP_PORT = '8081'
        DEPLOY_ENV_FILE = '/opt/reserve-by-sms-webhook/.env'
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
                    docker build \
                      -t "$APP_IMAGE:$BUILD_NUMBER" \
                      -t "$APP_IMAGE:latest" \
                      .
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

                    docker rm -f "$APP_CONTAINER" \
                      >/dev/null 2>&1 || true

                    docker run -d \
                      --name "$APP_CONTAINER" \
                      --restart unless-stopped \
                      --env-file "$DEPLOY_ENV_FILE" \
                      -p "$APP_PORT:8080" \
                      "$APP_IMAGE:$BUILD_NUMBER"

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
