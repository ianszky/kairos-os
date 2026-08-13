-- Waitlist emails for KaiOS landing page
CREATE TABLE IF NOT EXISTS public.waitlist_emails (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT NOT NULL UNIQUE,
  source TEXT DEFAULT 'landing',
  created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE INDEX IF NOT EXISTS waitlist_emails_created_at_idx
  ON public.waitlist_emails (created_at DESC);

ALTER TABLE public.waitlist_emails ENABLE ROW LEVEL SECURITY;

-- No public read; inserts handled server-side via service role.
-- Optional anon insert policy if switching to anon client later:
-- CREATE POLICY "Allow anonymous waitlist insert"
--   ON public.waitlist_emails FOR INSERT
--   TO anon
--   WITH CHECK (true);
