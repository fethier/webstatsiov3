# WebStats.io

A speed test web application that provides instant results on network metrics including latency, download speed, and upload speed.

## Architecture

- **Backend**: Java Spring Boot 3.2.0 with MongoDB
- **Frontend**: Angular 17 with Bootstrap 5
- **Database**: MongoDB for storing test results

## Prerequisites

### Backend Requirements
- Java 21 or higher
- Maven 3.6+
- MongoDB

### Frontend Requirements
- Node.js 18+ 
- npm 9+
- Angular CLI 17+

## Setup Instructions

### 1. Clone the Repository
```bash
git clone <repository-url>
cd WebStatsIoVibes
```

### 2. Backend Setup (Spring Boot)

#### Install Dependencies
```bash
mvn clean install
```

#### Configure MongoDB
Create `src/main/resources/application.properties` or `application.yml` with your MongoDB connection:
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/webstats
spring.data.mongodb.database=webstats
```

#### Run the Backend
```bash
# Development mode with auto-reload
mvn spring-boot:run

# Or build and run the JAR
mvn clean package
java -jar target/webstats-io-1.0.0.jar
```

The backend will start on `http://localhost:8080`

#### Test the Backend
```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

### 3. Frontend Setup (Angular)

#### Navigate to Frontend Directory
```bash
cd frontend
```

#### Install Dependencies
```bash
npm install
```

#### Run the Frontend
```bash
# Development server with hot reload
npm start
# or
ng serve

# Build for production
npm run build
# or
ng build --prod
```

The frontend will start on `http://localhost:4200`

#### Test the Frontend
```bash
# Run unit tests
npm test
# or
ng test

# Run tests in CI mode (single run)
ng test --watch=false --browsers=ChromeHeadless

# Run linting
npm run lint
# or
ng lint
```

## Development Workflow

### Running Both Services
1. Start MongoDB service
2. Start the Spring Boot backend: `mvn spring-boot:run`
3. In a new terminal, start the Angular frontend: `cd frontend && npm start`
4. Access the application at `http://localhost:4200`

### API Endpoints
The backend exposes REST APIs on `http://localhost:8080`. Common endpoints include:
- Health check: `GET /actuator/health`
- Speed test endpoints: `POST /api/speed-test/*`

### Database
MongoDB stores speed test results and user data. Default database name is `webstats`.

## Production Deployment

### Backend
```bash
mvn clean package
java -jar target/webstats-io-1.0.0.jar --spring.profiles.active=prod
```

### Frontend
```bash
cd frontend
ng build --configuration production
# Serve dist/webstats-frontend/ with a web server
```

## Troubleshooting

### Common Issues
- **Port conflicts**: Backend uses 8080, frontend uses 4200
- **MongoDB connection**: Ensure MongoDB is running and accessible
- **CORS issues**: Backend includes CORS configuration for development
- **Node/Java versions**: Ensure you meet the minimum version requirements

### Logs
- Backend logs: Available in console output and `logs/` directory
- Frontend logs: Browser developer console
- MongoDB logs: Check MongoDB service logs

## Contributing

1. Follow the existing code style and patterns
2. Run tests before committing
3. Update documentation for new features
4. Use meaningful commit messages