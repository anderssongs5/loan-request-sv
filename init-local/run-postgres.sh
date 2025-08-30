#!/bin/bash

# PostgreSQL Docker container setup for loan-request application
# Database: loan-request-db
# User: test
# Password: P@55w0rd
# Schema: test

docker run -d \
  --name loan-request-postgres \
  -e POSTGRES_DB=loan-request-db \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=P@55w0rd \
  -p 5432:5432 \
  postgres:latest

echo "PostgreSQL container 'loan-request-postgres' started successfully!"
echo "Database: loan-request-db"
echo "User: test"
echo "Password: P@55w0rd"
echo "Host: localhost"
echo "Port: 5432"
echo ""
echo "To connect: psql -h localhost -p 5432 -U test -d loan-request-db"
echo "To stop: docker stop loan-request-postgres"
echo "To remove: docker rm loan-request-postgres"