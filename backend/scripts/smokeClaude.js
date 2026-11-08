const { generateReply } = require('../src/services/claudeService');

async function main() {
  const reply = await generateReply({
    system: 'You are a concise kitchen assistant.',
    messages: [{ role: 'user', content: 'Give me one quick idea for using up 1 L of milk. One sentence.' }],
    maxTokens: 150,
  });
  console.log('Claude says:', reply);
}

main().catch((err) => {
  // Log only the status, type and message. Never the request or the key.
  console.error('Claude call failed:', err.status ?? '-', err.name, '-', err.message);
  process.exitCode = 1;
});