//Creates the Express application, configures middleware, and connects API routes

const express = require('express');
const chatRoutes = require('./routes/chatRoutes');
const aiRecipeRoutes = require('./routes/aiRecipeRoutes');
const healthRoutes = require('./routes/healthRoutes');
const recipeRoutes = require('./routes/recipeRoutes');
const productRoutes = require('./routes/productRoutes');
const notFound = require('./middleware/notFound');
const errorHandler = require('./middleware/errorHandler');
const { apiLimiter } = require('./middleware/rateLimiter');

const app = express();

app.disable('x-powered-by');
app.use(express.json({ limit: '100kb' }));
app.use('/api', apiLimiter);   // applies to every /api route

// Routes
app.use('/api/v1/health', healthRoutes);
app.use('/api/v1/ai/chat', chatRoutes);
app.use('/api/v1/ai/recipe', aiRecipeRoutes);
app.use('/api/v1/recipes', recipeRoutes);
app.use('/api/v1/products', productRoutes);

// These two must come AFTER all routes
app.use(notFound);      // no route matched → 404
app.use(errorHandler);  // must be last: catches every error

module.exports = app;