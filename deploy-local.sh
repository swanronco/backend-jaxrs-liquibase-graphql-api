#!/bin/bash
set -e

export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"

IMAGE="localhost/jaxrs-liquibase-graphql-api:latest"
CONTAINER="backend-local"
NETWORK="local"

echo "▶ Building WAR..."
./gradlew war

echo "▶ Building image..."
podman build -t "$IMAGE" .

echo "▶ Restarting container..."
podman rm -f "$CONTAINER" 2>/dev/null || true
podman run -d \
  --name "$CONTAINER" \
  --network "$NETWORK" \
  -e DB_URL=jdbc:postgresql://postgres-local:5432/testdb \
  -e DB_USER=postgres \
  -e DB_PASSWORD=password \
  -e JWT_SECRET=default-jwt-secret-key-for-dev-only-must-be-at-least-64-bytes-long! \
  -p 8080:8080 \
  "$IMAGE"

echo "✅ Done — http://localhost:8080/api/graphql"
