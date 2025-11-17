#include "persistence.h"
#include <fstream>
#include <sstream>
#include <iostream>

namespace persistence {

void save_users(const std::unordered_map<std::string, data::User>& users_by_name) {
  std::ofstream ofs("/tmp/chat_users.dat");
  if (!ofs) {
    std::cerr << "[ERROR] Failed to save users" << std::endl;
    return;
  }
  
  for (auto& p : users_by_name) {
    const data::User& u = p.second;
    ofs << u.id << "\t" << u.username << "\t" << u.password_hash << "\t" 
        << u.phone << "\n";
  }
  
  std::cout << "[PERSISTENCE] Saved " << users_by_name.size() << " users" << std::endl;
}

void load_users(
  std::unordered_map<std::string, data::User>& users_by_name,
  std::unordered_map<int, data::User>& users_by_id,
  std::unordered_map<int, std::unordered_map<int, bool>>& friends,
  std::unordered_map<int, std::vector<int>>& friend_requests,
  int& next_user_id
) {
  std::ifstream ifs("/tmp/chat_users.dat");
  if (!ifs) {
    std::cout << "[PERSISTENCE] No existing users file" << std::endl;
    return;
  }
  
  std::string line;
  int max_id = 0;
  int count = 0;
  
  while (std::getline(ifs, line)) {
    std::istringstream iss(line);
    data::User u;
    std::string tmp;
    
    std::getline(iss, tmp, '\t');
    u.id = std::stoi(tmp);
    std::getline(iss, u.username, '\t');
    std::getline(iss, u.password_hash, '\t');
    std::getline(iss, u.phone, '\t');
    u.socket_fd = -1;
    u.is_online = false;
    u.last_seen = 0;
    
    users_by_name[u.username] = u;
    users_by_id[u.id] = u;
    friends[u.id]; // Initialize empty maps
    friend_requests[u.id];
    
    if (u.id > max_id) max_id = u.id;
    count++;
  }
  
  next_user_id = max_id + 1;
  std::cout << "[PERSISTENCE] Loaded " << count << " users, next_id=" << next_user_id << std::endl;
}

} // namespace persistence
