import { POST } from './route';
import { NextRequest } from 'next/server';
import { describe, it, expect } from 'vitest';

describe('POST /api/prompt', () => {
  const createRequest = (body: any) => {
    return new NextRequest('http://localhost/api/prompt', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  };

  it('should return error if no intent provided', async () => {
    const req = createRequest({});
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(400);
    expect(json.type).toBe('ERROR');
    expect(json.text).toBe('No intent provided');
    expect(json.meta).toHaveProperty('timestamp');
  });

  it('should return ALARM_CONFIRM widget for alarm intent', async () => {
    const req = createRequest({ intent: 'Set an alarm for 7 AM' });
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(200);
    expect(json.type).toBe('WIDGET');
    expect(json.widget.widgetType).toBe('ALARM_CONFIRM');
    expect(json.meta).toHaveProperty('timestamp');
  });

  it('should return GENERIC_CARD widget for other intents', async () => {
    const req = createRequest({ prompt: 'Turn on the lights' });
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(200);
    expect(json.type).toBe('WIDGET');
    expect(json.widget.widgetType).toBe('GENERIC_CARD');
    expect(json.widget.items[0].primary).toBe('turn on the lights');
    expect(json.meta).toHaveProperty('timestamp');
  });

  it('should return 400 for invalid JSON', async () => {
    const req = new NextRequest('http://localhost/api/prompt', {
      method: 'POST',
      body: 'invalid-json',
    });
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(400);
    expect(json.type).toBe('ERROR');
    expect(json.text).toBe('Invalid JSON');
  });
});
