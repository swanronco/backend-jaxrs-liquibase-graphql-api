#!/bin/bash
set -e

export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"

IMAGE="localhost/jaxrs-liquibase-graphql-api:latest"
CONTAINER="backend-e2e"
NETWORK="e2e"

echo "▶ Building WAR..."
./gradlew war

echo "▶ Building image..."
podman build -t "$IMAGE" .

echo "▶ Restarting container..."
podman rm -f "$CONTAINER" 2>/dev/null || true
podman run -d \
  --name "$CONTAINER" \
  --network "$NETWORK" \
  -e DB_URL=jdbc:postgresql://postgres-e2e:5432/testdb \
  -e DB_USER=postgres \
  -e DB_PASSWORD=password \
  -p 8080:8080 \
  "$IMAGE"

echo "✅ Done — http://localhost:8080/jaxrs-liquibase-graphql-api/api/graphql"
