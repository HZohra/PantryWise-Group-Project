const { getChatReply } = require('../services/chatService');

/**
 * POST /api/v1/ai/chat
 *
 * By the time this runs, the request has already been:
 *   1. rate-limited (aiLimiter)
 *   2. validated and cleaned (validateBody(checkChatRequest))
 * so req.body is guaranteed to be { message, history, inventory, preferences, locale }.
 *
 * Errors from Claude are AppErrors. Express 5 forwards errors thrown in
 * async handlers to errorHandler automatically, so there's no try/catch here.
 *
 * Response: 200 { "reply": "...", "shoppingItems": [{ "name": "garlic", "amount": "2 cloves" }] }
 * shoppingItems is [] unless the user asked to add things to their shopping list;
 * the app asks the user to confirm before adding anything.
 */
async function chat(req, res) {
  const { reply, shoppingItems } = await getChatReply(req.body);
  res.status(200).json({ reply, shoppingItems });
}

module.exports = { chat };