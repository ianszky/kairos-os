import { createClient } from '@supabase/supabase-js';

// The admin client uses the service role key and bypasses all Row Level Security.
// Use this ONLY for server-side administrative tasks that require elevated permissions.
export const supabaseAdmin = createClient(
  process.env.NEXT_PUBLIC_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!, 
  {
    auth: {
      autoRefreshToken: false,
      persistSession: false
    }
  }
);
