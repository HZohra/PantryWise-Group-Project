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

// Express 5 passes listen errors (EACCES, EADDRINUSE...) to this callback; without checking it the
// server printed "running" and then exited silently.
app.listen(config.port, (err) => {                                      // CHANGED: PORT → config.port
  if (err) {
    console.error(`Cannot listen on port ${config.port}: ${err.code} ${err.message}`);
    console.error('Set another PORT in backend/.env (on Windows some ports are reserved: see "netsh interface ipv4 show excludedportrange protocol=tcp").');
    process.exit(1);
  }
  console.log(`PantryWise backend running at http://localhost:${config.port}`);
});