#include "utils.h"
#include <sstream>

namespace utils {

std::unordered_map<std::string, std::string> parse_kv(const std::string& s) {
  std::unordered_map<std::string, std::string> m;
  std::istringstream iss(s);
  std::string pair;
  while (std::getline(iss, pair, '&')) {
    auto pos = pair.find('=');
    if (pos != std::string::npos) {
      m[pair.substr(0, pos)] = pair.substr(pos + 1);
    }
  }
  return m;
}

std::string serialize_kv(const std::vector<std::pair<std::string, std::string>>& items) {
  std::ostringstream oss;
  for (size_t i = 0; i < items.size(); ++i) {
    if (i) oss << '&';
    oss << items[i].first << '=' << items[i].second;
  }
  return oss.str();
}

} // namespace utils
