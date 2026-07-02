import { NextRequest, NextResponse } from 'next/server';

export async function GET(req: NextRequest) {
  const userId = req.nextUrl.searchParams.get('userId');

  // Return the mock widget payload defined in TECHNICAL_IMPLEMENTATION_DOCUMENT.md
  return NextResponse.json({
    type: "WIDGET",
    widget: {
      widgetType: "DIGEST_SUMMARY",
      title: "Daily Digest — 23 notifications",
      items: [
        {
          id: "digest_social",
          primary: "Instagram: 5 likes, 2 comments",
          secondary: "Nothing requiring immediate action",
          icon: "social"
        },
        {
          id: "digest_promo",
          primary: "3 promotional emails archived",
          secondary: "Lazada, Shopee, GCash",
          icon: "mail_promo"
        }
      ]
    }
  });
}
