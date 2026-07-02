import { NextRequest, NextResponse } from 'next/server';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { packageName, title, text, category } = body;

    // Based on TECHNICAL_IMPLEMENTATION_DOCUMENT.md, return a mock classification for now
    return NextResponse.json({
      tier: "DIGEST",
      reason: "Social media engagement notification — non-critical"
    });
  } catch (error) {
    return NextResponse.json({ error: 'Invalid request' }, { status: 400 });
  }
}
