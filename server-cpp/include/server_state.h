#pragma once

#include <mutex>
#include <unordered_map>
#include <vector>
#include <atomic>
#include <memory>
#include "data_models.h"

// Forward declaration
namespace persistence {
  class PostgresPersistence;
}

namespace server_state {

// Global mutex for thread-safe access
extern std::mutex g_mutex;

// PostgreSQL persistence instance (nullptr if not initialized)
extern std::shared_ptr<persistence::PostgresPersistence> g_pg_persistence;

// User storage
extern std::unordered_map<std::string, data::User> g_users_by_name;
extern std::unordered_map<int, data::User> g_users_by_id;
extern std::atomic<int> g_next_user_id;

// Friend relationships
extern std::unordered_map<int, std::unordered_map<int, bool>> g_friends; // user->friend->true
extern std::unordered_map<int, std::vector<int>> g_friend_requests; // to_user -> [from_users]

// Online users
extern std::unordered_map<int, int> g_online_clients; // userId -> fd

// Groups
extern std::unordered_map<int, data::Group> g_groups; // groupId -> Group
extern std::atomic<int> g_next_group_id;

// Offline messages
extern std::unordered_map<int, std::vector<data::PendingMsg>> g_offline_messages; // userId -> messages

// Helper functions
void enqueue_offline(int userId, uint8_t type, const std::string& payload, uint32_t fromUserId);
void notify_friends_status_change(int userId, bool online);

} // namespace server_state
