-- ============================================================
-- VigiPro Phase 5: Multi-Tenancy Schema
-- Sites, Members, Invitations with RLS
-- ============================================================

-- ========================
-- 1. CREATE ALL TABLES
-- ========================

-- Sites
CREATE TABLE IF NOT EXISTS sites (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  address TEXT,
  owner_id UUID NOT NULL REFERENCES auth.users(id),
  created_at TIMESTAMPTZ DEFAULT now()
);

-- Site members
CREATE TABLE IF NOT EXISTS site_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id),
  role TEXT NOT NULL CHECK (role IN ('owner','admin','viewer','time_restricted','guest')),
  invited_by UUID REFERENCES auth.users(id),
  valid_from TIMESTAMPTZ,
  valid_until TIMESTAMPTZ,
  UNIQUE(site_id, user_id)
);

-- Invitations
CREATE TABLE IF NOT EXISTS invitations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
  invite_code TEXT NOT NULL UNIQUE,
  role TEXT NOT NULL CHECK (role IN ('viewer','time_restricted','guest')),
  camera_ids UUID[],
  time_start TIME,
  time_end TIME,
  days_of_week INT[],
  max_uses INT DEFAULT 1,
  uses_count INT DEFAULT 0,
  expires_at TIMESTAMPTZ NOT NULL,
  created_by UUID REFERENCES auth.users(id)
);

-- ========================
-- 2. ENABLE RLS
-- ========================

ALTER TABLE sites ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitations ENABLE ROW LEVEL SECURITY;

-- ========================
-- 3. RLS POLICIES
-- ========================

-- Sites: owner has full access
CREATE POLICY "owner_full_access" ON sites
  FOR ALL USING (auth.uid() = owner_id);

-- Sites: members can read
CREATE POLICY "member_read" ON sites
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM site_members
      WHERE site_id = sites.id AND user_id = auth.uid()
    )
  );

-- Site members: members can see co-members
CREATE POLICY "member_read_own_site" ON site_members
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM site_members sm
      WHERE sm.site_id = site_members.site_id AND sm.user_id = auth.uid()
    )
  );

-- Site members: admins/owners can manage
CREATE POLICY "admin_manage" ON site_members
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM site_members sm
      WHERE sm.site_id = site_members.site_id
        AND sm.user_id = auth.uid()
        AND sm.role IN ('owner','admin')
    )
  );

-- Invitations: admins/owners can manage
CREATE POLICY "admin_manage_invites" ON invitations
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM site_members sm
      WHERE sm.site_id = invitations.site_id
        AND sm.user_id = auth.uid()
        AND sm.role IN ('owner','admin')
    )
  );

-- Invitations: anyone authenticated can read (needed to redeem)
CREATE POLICY "anyone_read_by_code" ON invitations
  FOR SELECT USING (true);

-- ========================
-- 4. RPC: REDEEM INVITATION
-- ========================

CREATE OR REPLACE FUNCTION redeem_invitation(p_code TEXT)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_invite invitations%ROWTYPE;
  v_member_id UUID;
BEGIN
  SELECT * INTO v_invite FROM invitations WHERE invite_code = p_code;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Convite nao encontrado';
  END IF;

  IF v_invite.expires_at < now() THEN
    RAISE EXCEPTION 'Convite expirado';
  END IF;

  IF v_invite.uses_count >= v_invite.max_uses THEN
    RAISE EXCEPTION 'Convite esgotado';
  END IF;

  INSERT INTO site_members (site_id, user_id, role, invited_by, valid_from, valid_until)
  VALUES (
    v_invite.site_id,
    auth.uid(),
    v_invite.role,
    v_invite.created_by,
    CASE WHEN v_invite.time_start IS NOT NULL THEN now() END,
    v_invite.expires_at
  )
  ON CONFLICT (site_id, user_id) DO NOTHING
  RETURNING id INTO v_member_id;

  IF v_member_id IS NULL THEN
    RETURN json_build_object('status', 'already_member');
  END IF;

  UPDATE invitations SET uses_count = uses_count + 1 WHERE id = v_invite.id;

  RETURN json_build_object('status', 'success', 'site_id', v_invite.site_id);
END;
$$;

-- ========================
-- 5. INDEXES
-- ========================

CREATE INDEX IF NOT EXISTS idx_site_members_site_id ON site_members(site_id);
CREATE INDEX IF NOT EXISTS idx_site_members_user_id ON site_members(user_id);
CREATE INDEX IF NOT EXISTS idx_invitations_site_id ON invitations(site_id);
CREATE INDEX IF NOT EXISTS idx_invitations_code ON invitations(invite_code);
CREATE INDEX IF NOT EXISTS idx_sites_owner ON sites(owner_id);
