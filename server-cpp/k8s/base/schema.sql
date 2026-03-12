-- Chat Application Database Schema

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Friends relationship table
CREATE TABLE IF NOT EXISTS friends (
    id SERIAL PRIMARY KEY,
    user_id_1 INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_2 INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_friendship UNIQUE(user_id_1, user_id_2),
    CONSTRAINT user_order_check CHECK (user_id_1 < user_id_2)
);

-- Friend requests table
CREATE TABLE IF NOT EXISTS friend_requests (
    id SERIAL PRIMARY KEY,
    from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_friend_request UNIQUE(from_user_id, to_user_id)
);

-- Direct messages table
CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW(),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Groups table
CREATE TABLE IF NOT EXISTS groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Group members table
CREATE TABLE IF NOT EXISTS group_members (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_group_membership UNIQUE(group_id, user_id)
);

-- Group messages table
CREATE TABLE IF NOT EXISTS group_messages (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Activity logs table
CREATE TABLE IF NOT EXISTS activity_logs (
    id SERIAL PRIMARY KEY,
    log_type VARCHAR(50) NOT NULL,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    target_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Offline messages table
CREATE TABLE IF NOT EXISTS offline_messages (
    id SERIAL PRIMARY KEY,
    to_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type INTEGER NOT NULL,
    payload TEXT NOT NULL,
    from_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    delivered BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create view for friends (bidirectional relationship)
CREATE OR REPLACE VIEW v_user_friends AS
SELECT user_id_1 as user_id, user_id_2 as friend_id FROM friends
UNION ALL
SELECT user_id_2 as user_id, user_id_1 as friend_id FROM friends;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_online ON users(is_online);
CREATE INDEX IF NOT EXISTS idx_friends_user1 ON friends(user_id_1);
CREATE INDEX IF NOT EXISTS idx_friends_user2 ON friends(user_id_2);
CREATE INDEX IF NOT EXISTS idx_friend_requests_to ON friend_requests(to_user_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_requests_from ON friend_requests(from_user_id, status);
CREATE INDEX IF NOT EXISTS idx_messages_to ON messages(to_user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_messages_from ON messages(from_user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_group_members_group ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user ON group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_group ON group_messages(group_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_offline_messages_user ON offline_messages(to_user_id, delivered);

-- Insert default users if they don't exist
INSERT INTO users (username, password_hash, phone) VALUES 
('dat', 'c273fa7505428a152554ab1248060934d8398ee3c5097410c92e4a4cf6986011', '123456789'),
('alice', 'c273fa7505428a152554ab1248060934d8398ee3c5097410c92e4a4cf6986011', '987654321')
ON CONFLICT (username) DO NOTHING;