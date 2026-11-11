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
  console.error('Mapped to:', err.statusCode, err.code, '-', err.message);
  process.exitCode = 1;
});