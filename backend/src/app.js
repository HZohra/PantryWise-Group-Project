//Creates the Express application, configures middleware, and connects API routes

const express = require('express');
const healthRoutes = require('./routes/healthRoutes');
const app = express();

app.disable('x-powered-by');                 // don't advertise "Express" in response headers
app.use(express.json({ limit: '100kb' }));   // parse JSON bodies; reject huge ones

app.use('/api/v1/health', healthRoutes);

module.exports = app;

