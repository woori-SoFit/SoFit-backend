#!/bin/bash
echo ">>> Stopping sofit-user-api container..."
docker stop sofit-user-api || true
docker rm sofit-user-api || true
echo ">>> Container stopped."
