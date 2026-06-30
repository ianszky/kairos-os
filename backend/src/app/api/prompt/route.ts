import { NextRequest, NextResponse } from 'next/server';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const intentStr = (body.intent || body.prompt || '').toLowerCase();

    if (!intentStr) {
      return NextResponse.json({ type: 'ERROR', text: 'No intent provided', meta: { timestamp: new Date().toISOString() } }, { status: 400 });
    }

    if (intentStr.includes('alarm')) {
      return NextResponse.json({ type: 'WIDGET', widget: { widgetType: 'ALARM_CONFIRM', title: 'Set Alarm', items: [{ id: '1', primary: 'Set alarm?', icon: 'alarm' }], actions: [{ label: 'Open Clock', actionType: 'DEEP_LINK', target: 'clock://alarm' }] }, meta: { timestamp: new Date().toISOString() } });
    }

    return NextResponse.json({ type: 'WIDGET', widget: { widgetType: 'GENERIC_CARD', title: 'Command', items: [{ id: '1', primary: intentStr, icon: 'info' }], actions: [{ label: 'Dismiss', actionType: 'DISMISS', target: '' }] }, meta: { timestamp: new Date().toISOString() } });
  } catch (e) {
    return NextResponse.json({ type: 'ERROR', text: 'Invalid JSON', meta: { timestamp: new Date().toISOString() } }, { status: 400 });
  }
}
