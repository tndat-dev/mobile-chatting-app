#include "server_state.h"
#include "network_utils.h"
#include "utils.h"
#include "protocol.h"
#include <iostream>

namespace server_state {

// Define global variables
std::mutex g_mutex;
std::shared_ptr<persistence::PostgresPersistence> g_pg_persistence = nullptr;
std::unordered_map<std::string, data::User> g_users_by_name;
std::unordered_map<int, data::User> g_users_by_id;
std::atomic<int> g_next_user_id{1};
std::unordered_map<int, std::unordered_map<int, bool>> g_friends;
std::unordered_map<int, std::vector<int>> g_friend_requests;
std::unordered_map<int, int> g_online_clients;
std::unordered_map<int, data::Group> g_groups;
std::atomic<int> g_next_group_id{1};
std::unordered_map<int, std::vector<data::PendingMsg>> g_offline_messages;

void enqueue_offline(int userId, uint8_t type, const std::string& payload, uint32_t fromUserId) {
  std::lock_guard<std::mutex> lock(g_mutex);
  g_offline_messages[userId].push_back(data::PendingMsg{type, payload, fromUserId});
}

void notify_friends_status_change(int userId, bool online) {
  std::vector<int> friends;
  {
    std::lock_guard<std::mutex> lock(g_mutex);
    for (auto& kv : g_friends[userId]) {
      if (kv.second) friends.push_back(kv.first);
    }
  }
  
  for (int fid : friends) {
    int fd = -1;
    {
      std::lock_guard<std::mutex> lock(g_mutex);
      auto it = g_online_clients.find(fid);
      if (it != g_online_clients.end()) fd = it->second;
    }
    if (fd != -1) {
      auto payload = utils::serialize_kv({{"userId", std::to_string(userId)}});
      network::send_message(fd, online ? proto::USER_ONLINE : proto::USER_OFFLINE, payload, userId);
    }
  }
}

} // namespace server_state
