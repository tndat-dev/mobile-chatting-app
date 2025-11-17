#pragma once

#include <unordered_map>
#include <string>
#include "data_models.h"

namespace persistence {

// Save all users to disk
void save_users(
  const std::unordered_map<std::string, data::User>& users_by_name
);

// Load users from disk
void load_users(
  std::unordered_map<std::string, data::User>& users_by_name,
  std::unordered_map<int, data::User>& users_by_id,
  std::unordered_map<int, std::unordered_map<int, bool>>& friends,
  std::unordered_map<int, std::vector<int>>& friend_requests,
  int& next_user_id
);

} // namespace persistence
