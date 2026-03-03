-- ============================================================
-- Fix: RLS infinite recursion on site_members
-- Problem: site_members policies query site_members → infinite loop
-- Solution: SECURITY DEFINER helpers + simpler policies + auto-owner trigger
-- ============================================================

-- ========================
-- 1. HELPER FUNCTIONS (bypass RLS)
-- ========================

CREATE OR REPLACE FUNCTION is_site_member(p_site_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM site_members
    WHERE site_id = p_site_id AND user_id = auth.uid()
  );
$$;

CREATE OR REPLACE FUNCTION is_site_admin(p_site_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM site_members
    WHERE site_id = p_site_id
      AND user_id = auth.uid()
      AND role IN ('owner', 'admin')
  );
$$;

-- ========================
-- 2. DROP OLD RECURSIVE POLICIES
-- ========================

DROP POLICY IF EXISTS "member_read" ON sites;
DROP POLICY IF EXISTS "member_read_own_site" ON site_members;
DROP POLICY IF EXISTS "admin_manage" ON site_members;
DROP POLICY IF EXISTS "admin_manage_invites" ON invitations;

-- ========================
-- 3. RE-CREATE NON-RECURSIVE POLICIES
-- ========================

-- Sites: members can read (uses helper function, no recursion)
CREATE POLICY "member_read" ON sites
  FOR SELECT USING (is_site_member(id));

-- Site members: users can see their own memberships (no subquery = no recursion)
CREATE POLICY "member_read_own_site" ON site_members
  FOR SELECT USING (user_id = auth.uid());

-- Site members: admins can see ALL members of their sites
CREATE POLICY "admin_read_all_members" ON site_members
  FOR SELECT USING (is_site_admin(site_id));

-- Site members: admins/owners can insert/update/delete
CREATE POLICY "admin_manage_members" ON site_members
  FOR INSERT WITH CHECK (is_site_admin(site_id));

CREATE POLICY "admin_update_members" ON site_members
  FOR UPDATE USING (is_site_admin(site_id));

CREATE POLICY "admin_delete_members" ON site_members
  FOR DELETE USING (is_site_admin(site_id));

-- Site owner can insert members (needed for initial owner membership via trigger)
CREATE POLICY "owner_insert_member" ON site_members
  FOR INSERT WITH CHECK (
    EXISTS (SELECT 1 FROM sites WHERE id = site_id AND owner_id = auth.uid())
  );

-- Invitations: admins/owners can manage (uses helper function)
CREATE POLICY "admin_manage_invites" ON invitations
  FOR ALL USING (is_site_admin(site_id));

-- ========================
-- 4. AUTO-CREATE OWNER MEMBERSHIP ON SITE CREATION
-- ========================

CREATE OR REPLACE FUNCTION create_owner_membership()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO site_members (site_id, user_id, role)
  VALUES (NEW.id, NEW.owner_id, 'owner')
  ON CONFLICT (site_id, user_id) DO NOTHING;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS auto_create_owner_membership ON sites;

CREATE TRIGGER auto_create_owner_membership
  AFTER INSERT ON sites
  FOR EACH ROW
  EXECUTE FUNCTION create_owner_membership();
