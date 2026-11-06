-- Enable Row Level Security (RLS) on Eyes tables
alter table public.eyes_logs enable row level security;
alter table public.eyes_commands enable row level security;

-- Policy for anonymous/authenticated read & write access (adjust based on production needs)
create policy "Allow all access to eyes_logs" on public.eyes_logs for all using (true) with check (true);
create policy "Allow all access to eyes_commands" on public.eyes_commands for all using (true) with check (true);
