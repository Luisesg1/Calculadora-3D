-- user_backups: snapshot completo del dataset local por usuario (JSON en columna payload).
-- Modelo last-write-wins por cuenta. Correr en Supabase → SQL Editor. Idempotente.

create table if not exists public.user_backups (
    user_id    uuid primary key references auth.users (id) on delete cascade,
    payload    text not null,
    updated_at timestamptz not null default now()
);

-- Refresca updated_at en cada upsert.
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists user_backups_set_updated_at on public.user_backups;
create trigger user_backups_set_updated_at
    before update on public.user_backups
    for each row execute function public.set_updated_at();

-- RLS: cada usuario ve/escribe solo su propio backup.
alter table public.user_backups enable row level security;

drop policy if exists "user_backups_select_own" on public.user_backups;
create policy "user_backups_select_own" on public.user_backups
    for select using (auth.uid() = user_id);

drop policy if exists "user_backups_insert_own" on public.user_backups;
create policy "user_backups_insert_own" on public.user_backups
    for insert with check (auth.uid() = user_id);

drop policy if exists "user_backups_update_own" on public.user_backups;
create policy "user_backups_update_own" on public.user_backups
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
