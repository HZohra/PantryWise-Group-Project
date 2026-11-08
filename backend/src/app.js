//Creates the Express application, configures middleware, and connects API routes

const express = require('express');
const healthRoutes = require('./routes/healthRoutes');
const notFound = require('./middleware/notFound');
const errorHandler = require('./middleware/errorHandler');

const app = express();

app.disable('x-powered-by');
app.use(express.json({ limit: '100kb' }));

// Routes
app.use('/api/v1/health', healthRoutes);

// These two must come AFTER all routes
app.use(notFound);      // no route matched → 404
app.use(errorHandler);  // must be last: catches every error

module.exports = app;