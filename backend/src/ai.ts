import { config } from './config.js';
import type { SummaryPayload } from './types.js';

function buildPrompt(text: string): string {
  return [
    'You are a concise summarization assistant.',
    '',
    'Summarize the following text in 2-3 sentences and extract 2-4 relevant tags.',
    '',
    'Return ONLY valid JSON in this exact format (no markdown, no code blocks):',
    '{',
    '  "summary": "Your 2-3 sentence summary here.",',
    '  "tags": ["tag1", "tag2", "tag3"]',
    '}',
    '',
    'TEXT TO SUMMARIZE:',
    text,
  ].join('\n');
}

function cleanResponseText(raw: string): string {
  let cleaned = raw.trim();
  if (cleaned.startsWith('```json')) {
    cleaned = cleaned.slice(7);
  }
  if (cleaned.startsWith('```')) {
    cleaned = cleaned.slice(3);
  }
  if (cleaned.endsWith('```')) {
    cleaned = cleaned.slice(0, -3);
  }
  return cleaned.trim();
}

export async function summarizeText(text: string): Promise<SummaryPayload> {
  if (!config.geminiApiKey) {
    throw new Error('GEMINI_API_KEY is not configured');
  }

  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${config.geminiModel}:generateContent?key=${config.geminiApiKey}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        contents: [{ parts: [{ text: buildPrompt(text) }] }],
        generationConfig: { temperature: 0.3, responseMimeType: 'application/json' },
      }),
    },
  );

  if (!response.ok) {
    throw new Error(`Gemini API error: HTTP ${response.status} ${await response.text()}`.trim());
  }

  const data = (await response.json()) as {
    candidates?: Array<{
      content?: {
        parts?: Array<{ text?: string }>;
      };
    }>;
  };
  const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text;

  if (typeof rawText !== 'string' || !rawText.trim()) {
    throw new Error('Empty response from Gemini');
  }

  return JSON.parse(cleanResponseText(rawText)) as SummaryPayload;
}