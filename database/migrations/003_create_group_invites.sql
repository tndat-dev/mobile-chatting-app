-- Migration: Add group_invites table to support invitation workflow

CREATE TABLE IF NOT EXISTS group_invites (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_group_invites_to_user ON group_invites(to_user_id, status);
CREATE INDEX idx_group_invites_group ON group_invites(group_id);
