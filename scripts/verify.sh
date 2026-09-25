#!/usr/bin/env sh
set -eu

if [ -f backend/pom.xml ]; then
  (cd backend && ./mvnw test)
else
  echo "Backend absent : vérification Maven ignorée."
fi

if [ -f frontend/package.json ]; then
  (cd frontend && npm ci && npm run build)
else
  echo "Frontend absent : vérification npm ignorée."
fi
