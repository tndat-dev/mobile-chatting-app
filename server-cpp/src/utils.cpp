#include "utils.h"
#include <sstream>
#include <vector>

static const std::string b64_chars =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789+/";

std::string utils::base64_encode(const std::string& in) {
  std::string out;
  int val = 0, valb = -6;
  for (unsigned char c : in) {
    val = (val << 8) + c;
    valb += 8;
    while (valb >= 0) {
      out.push_back(b64_chars[(val >> valb) & 0x3F]);
      valb -= 6;
    }
  }
  if (valb > -6) out.push_back(b64_chars[((val << 8) >> (valb + 8)) & 0x3F]);
  while (out.size() % 4) out.push_back('=');
  return out;
}

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
