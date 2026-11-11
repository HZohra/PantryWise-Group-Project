//Starts the server and makes it listen for incoming requests on a specific port.

const { config, findMissingEnv } = require('./config/env'); // NEW: loads .env + gives us config
const app = require('./app');

// NEW: stop here if a key is missing
const missing = findMissingEnv();
if (missing.length > 0) {
  console.error(`Missing required environment variables: ${missing.join(', ')}`);
  console.error('Copy .env.example to .env and fill in the values.');
  process.exit(1);
}

app.listen(config.port, () => {                                         // CHANGED: PORT → config.port
  console.log(`PantryWise backend running at http://localhost:${config.port}`);
});