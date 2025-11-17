-- ====================================================================
-- Chat Application Database Schema
-- PostgreSQL Migration Script
-- Date: 2025-11-13
-- ====================================================================

-- Create database (run manually if needed)
-- CREATE DATABASE chat_app;
-- \c chat_app;

-- ====================================================================
-- 1. USERS TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(64) NOT NULL,  -- SHA-256 hash
    phone VARCHAR(20),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen BIGINT DEFAULT 0,  -- Unix timestamp in milliseconds
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_online ON users(is_online);

-- ====================================================================
-- 2. FRIENDS TABLE (Bidirectional friendship)
-- ====================================================================
CREATE TABLE IF NOT EXISTS friends (
    id SERIAL PRIMARY KEY,
    user_id_1 INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_2 INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id_1, user_id_2),
    CHECK (user_id_1 < user_id_2)  -- Ensure canonical ordering
);

CREATE INDEX idx_friends_user1 ON friends(user_id_1);
CREATE INDEX idx_friends_user2 ON friends(user_id_2);

-- ====================================================================
-- 3. FRIEND REQUESTS TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS friend_requests (
    id SERIAL PRIMARY KEY,
    from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, ACCEPTED, DECLINED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(from_user_id, to_user_id),
    CHECK (from_user_id != to_user_id)
);

CREATE INDEX idx_friend_requests_to_user ON friend_requests(to_user_id, status);
CREATE INDEX idx_friend_requests_from_user ON friend_requests(from_user_id);

-- ====================================================================
-- 4. DIRECT MESSAGES TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_from_user ON messages(from_user_id);
CREATE INDEX idx_messages_to_user ON messages(to_user_id);
CREATE INDEX idx_messages_conversation ON messages(from_user_id, to_user_id, timestamp);
CREATE INDEX idx_messages_timestamp ON messages(timestamp DESC);

-- ====================================================================
-- 5. GROUPS TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_groups_creator ON groups(creator_id);

-- ====================================================================
-- 6. GROUP MEMBERS TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS group_members (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);

-- ====================================================================
-- 7. GROUP MESSAGES TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS group_messages (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_group_messages_group ON group_messages(group_id, timestamp DESC);
CREATE INDEX idx_group_messages_user ON group_messages(user_id);

-- ====================================================================
-- 8. ACTIVITY LOGS TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS activity_logs (
    id SERIAL PRIMARY KEY,
    log_type VARCHAR(50) NOT NULL,  -- LOGIN, LOGOUT, FRIEND_REQUEST, etc.
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    target_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    details TEXT,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_logs_type ON activity_logs(log_type);
CREATE INDEX idx_activity_logs_user ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp DESC);

-- ====================================================================
-- 9. OFFLINE MESSAGES QUEUE (for delivery when user comes online)
-- ====================================================================
CREATE TABLE IF NOT EXISTS offline_messages (
    id SERIAL PRIMARY KEY,
    to_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type INTEGER NOT NULL,  -- Message type code
    payload TEXT NOT NULL,
    from_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_offline_messages_to_user ON offline_messages(to_user_id, delivered);

-- ====================================================================
-- HELPER FUNCTIONS
-- ====================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_friend_requests_updated_at BEFORE UPDATE ON friend_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ====================================================================
-- VIEWS FOR COMMON QUERIES
-- ====================================================================

-- View: Get all friends for a user with their online status
CREATE OR REPLACE VIEW v_user_friends AS
SELECT 
    f.user_id_1 as user_id,
    u2.id as friend_id,
    u2.username as friend_username,
    u2.is_online,
    u2.last_seen,
    f.created_at as friendship_since
FROM friends f
JOIN users u2 ON f.user_id_2 = u2.id
UNION ALL
SELECT 
    f.user_id_2 as user_id,
    u1.id as friend_id,
    u1.username as friend_username,
    u1.is_online,
    u1.last_seen,
    f.created_at as friendship_since
FROM friends f
JOIN users u1 ON f.user_id_1 = u1.id;

-- View: Get pending friend requests for a user
CREATE OR REPLACE VIEW v_pending_requests AS
SELECT 
    fr.id,
    fr.from_user_id,
    u1.username as from_username,
    fr.to_user_id,
    u2.username as to_username,
    fr.created_at
FROM friend_requests fr
JOIN users u1 ON fr.from_user_id = u1.id
JOIN users u2 ON fr.to_user_id = u2.id
WHERE fr.status = 'PENDING';

-- ====================================================================
-- SEED DATA (Optional - for testing)
-- ====================================================================

-- Insert test users
-- INSERT INTO users (username, password, phone) VALUES
-- ('alice', 'hash1', '1234567890'),
-- ('bob', 'hash2', '0987654321');

-- ====================================================================
-- GRANTS (Adjust as needed for your setup)
-- ====================================================================

-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO chat_app_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO chat_app_user;

-- ====================================================================
-- MIGRATION COMPLETE
-- ====================================================================

SELECT 'Database schema created successfully!' as status;
