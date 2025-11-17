#pragma once

#include <string>
#include <unordered_map>
#include <vector>
#include <utility>

namespace utils {

// Parse key-value pairs from "key1=value1&key2=value2" format
std::unordered_map<std::string, std::string> parse_kv(const std::string& s);

// Serialize key-value pairs to "key1=value1&key2=value2" format
std::string serialize_kv(const std::vector<std::pair<std::string, std::string>>& items);

} // namespace utils
