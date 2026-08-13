import { z } from "zod";
import { getSupabaseAdmin } from "@/lib/supabase/admin";

export const waitlistSchema = z.object({
  email: z.string().trim().email("Enter a valid email address"),
  source: z.string().trim().max(64).optional().default("landing"),
});

export type WaitlistInput = z.infer<typeof waitlistSchema>;

export async function addToWaitlist(input: WaitlistInput) {
  const { email, source } = waitlistSchema.parse(input);

  const { error } = await getSupabaseAdmin().from("waitlist_emails").insert({
    email: email.toLowerCase(),
    source,
  });

  if (error) {
    if (error.code === "23505") {
      return { ok: false as const, reason: "duplicate" as const };
    }
    throw error;
  }

  return { ok: true as const };
}
