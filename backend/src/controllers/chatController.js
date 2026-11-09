/**
 * POST /api/v1/ai/chat
 * STUB: echoes the validated body. Phase 4.3 replaces this with a real Claude call.
 */
async function chat(req, res) {
  const { message, history, inventory } = req.body;
  res.json({
    reply: `(stub) You said: "${message}". History: ${history.length} messages, inventory: ${inventory.length} items.`,
  });
}

module.exports = { chat };