# Docker Staging Environment

This directory contains Docker configurations for running the WebStats.io application in a staging environment.

## Quick Start

1. **Copy the environment template:**
   ```bash
   cp .env.staging.template .env.staging
   ```

2. **Update passwords in `.env.staging`** (recommended for security)

3. **Start the staging environment:**
   ```bash
   docker-compose -f docker-compose.staging.yml --env-file .env.staging up -d
   ```

4. **Access the application:**
   - Frontend: http://localhost
   - Backend API: http://localhost:8080
   - MongoDB: localhost:27017

## Services

### MongoDB
- **Image:** mongo:7.0
- **Security:** Authentication enabled with custom users
- **Data:** Persisted in named volume `mongodb_staging_data`
- **Port:** 27017

### Backend (Spring Boot)
- **Build:** From local Dockerfile
- **Port:** 8080
- **Health Check:** `/actuator/health`
- **Profile:** staging

### Frontend (Angular + Nginx)
- **Build:** From frontend/Dockerfile
- **Port:** 80
- **Features:** Gzip compression, security headers, API proxy

## Environment Variables

| Variable | Default          | Description |
|----------|------------------|-------------|
| `MONGO_ROOT_USERNAME` | webstats_admin   | MongoDB root user |
| `MONGO_ROOT_PASSWORD` | xxxxxxx         | MongoDB root password |
| `MONGO_DATABASE` | webstats_staging | Application database |
| `MONGO_APP_USERNAME` | webstats_user    | Application user |
| `MONGO_APP_PASSWORD` | xxxxxx           | Application password |

## Commands

```bash
# Start services
docker-compose -f docker-compose.staging.yml up -d

# View logs
docker-compose -f docker-compose.staging.yml logs -f

# Stop services
docker-compose -f docker-compose.staging.yml down

# Rebuild and restart
docker-compose -f docker-compose.staging.yml up -d --build

# Clean up (removes volumes)
docker-compose -f docker-compose.staging.yml down -v
```

## Security Features

- MongoDB runs with authentication enabled
- Separate application user with limited permissions
- Non-root users in containers
- Security headers in Nginx
- Health checks for all services
- Isolated network for services