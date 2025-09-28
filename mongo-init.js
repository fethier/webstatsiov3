// MongoDB initialization script for staging environment
print('Starting MongoDB initialization...');

// Switch to the application database
db = db.getSiblingDB(process.env.MONGO_INITDB_DATABASE || 'webstats_staging');

// Create application user with read/write permissions
db.createUser({
  user: process.env.MONGO_APP_USERNAME || 'webstats_user',
  pwd: process.env.MONGO_APP_PASSWORD || '09j82f3doijnwedfknj9812',
  roles: [
    {
      role: 'readWrite',
      db: process.env.MONGO_INITDB_DATABASE || 'webstats_staging'
    }
  ]
});

// Create indexes for better performance
db.speedTestResults.createIndex({ "timestamp": 1 });
db.speedTestResults.createIndex({ "userId": 1, "timestamp": -1 });
db.speedTestResults.createIndex({ "ipAddress": 1 });

// Create indexes for speed test sessions
db.speed_test_sessions.createIndex({ "sessionId": 1 });
db.speed_test_sessions.createIndex({ "timestamp": 1 });

// Create a sample collection if it doesn't exist
db.speedTestResults.insertOne({
  _id: ObjectId(),
  timestamp: new Date(),
  downloadSpeed: 0,
  uploadSpeed: 0,
  latency: 0,
  jitter: 0,
  ipAddress: "127.0.0.1",
  userAgent: "System",
  isInitialData: true
});

// Create a sample speed test session to ensure collection exists
db.speed_test_sessions.insertOne({
  _id: ObjectId(),
  sessionId: "init-session",
  timestamp: new Date(),
  status: "completed",
  isInitialData: true
});

print('MongoDB initialization completed successfully.');