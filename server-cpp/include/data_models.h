#pragma once

#include <string>
#include <unordered_map>
#include <vector>
#include <cstdint>

namespace data {

struct User {
  int id;
  std::string username;
  std::string password_hash;
  std::string phone;
  int socket_fd; // For server runtime
  bool is_online; // For database
  long long last_seen; // For database
};

struct Group {
  int id;
  std::string name;
  int ownerId;
  std::unordered_map<int, bool> members;
  // Optional per-member nickname map (runtime only)
  std::unordered_map<int, std::string> member_nicknames;
};

struct Message {
  int id;
  int from_user_id;
  int to_user_id;
  std::string content;
  long long timestamp;
  bool is_read;
};

struct FriendRequest {
  int id;
  int from_user_id;
  int to_user_id;
  std::string status; // PENDING, ACCEPTED, DECLINED
  long long timestamp;
};

struct GroupInvite {
  int id;
  int group_id;
  int from_user_id;
  int to_user_id;
  std::string status; // PENDING, ACCEPTED, DECLINED
  long long timestamp;
};

struct GroupMessage {
  int id;
  int group_id;
  int user_id;
  std::string content;
  long long timestamp;
};

struct PendingMsg {
  uint8_t type;
  std::string payload;
  uint32_t fromUserId;
};

} // namespace data
