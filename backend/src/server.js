//Starts the server and makes it listen for incoming requests on a specific port.

require('dotenv').config({ quiet: true });   // load .env into process.env (quiet = no startup log line)
const app = require('./app');
const PORT = Number(process.env.PORT) || 3000;

app.listen(PORT, () => {
  console.log(`PantryWise backend running at http://localhost:${PORT}`);
});

