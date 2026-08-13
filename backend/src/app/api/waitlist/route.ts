import { NextResponse } from "next/server";
import { ZodError } from "zod";
import { addToWaitlist, waitlistSchema } from "@/lib/waitlist";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const input = waitlistSchema.parse(body);
    const result = await addToWaitlist(input);

    if (!result.ok) {
      return NextResponse.json(
        { error: "This email is already on the waitlist." },
        { status: 409 },
      );
    }

    return NextResponse.json({ ok: true });
  } catch (error) {
    if (error instanceof ZodError) {
      return NextResponse.json(
        { error: error.issues[0]?.message ?? "Invalid request." },
        { status: 400 },
      );
    }

    console.error("[waitlist]", error);
    return NextResponse.json(
      { error: "Something went wrong. Try again in a moment." },
      { status: 500 },
    );
  }
}
