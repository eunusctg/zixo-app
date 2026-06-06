/**
 * Supabase Client Configuration for Zixo
 * Replaces Firebase as the primary backend
 */

import { createClient } from '@supabase/supabase-js';

// ==================== SUPABASE CONFIGURATION ====================
const SUPABASE_URL = 'https://vgdobfnuhjvyjxmbtosm.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZnZG9iZm51aGp2eWp4bWJ0b3NtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA3NDEyMTQsImV4cCI6MjA5NjMxNzIxNH0.pR1fGBlcYNxSITxN3T1nO9sIPQzt1EM92Z4eUeeoKJY';

// Service role key - ONLY use in API routes (server-side), never in client
const SUPABASE_SERVICE_ROLE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZnZG9iZm51aGp2eWp4bWJ0b3NtIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4MDc0MTIxNCwiZXhwIjoyMDk2MzE3MjE0fQ.Kmk7NoPQy_U8kw7XoXQKU18PfpVdkOYtt1ksAJdJIcc';

// ==================== CLIENT-SIDE SUPABASE ====================
export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
  auth: {
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true,
  },
  realtime: {
    params: {
      eventsPerSecond: 10,
    },
  },
  db: {
    schema: 'public',
  },
});

// ==================== SERVER-SIDE SUPABASE (for API routes) ====================
export const supabaseAdmin = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
  auth: {
    autoRefreshToken: false,
    persistSession: false,
  },
});

// ==================== EXPORTS ====================
export default supabase;

export { SUPABASE_URL, SUPABASE_ANON_KEY };
