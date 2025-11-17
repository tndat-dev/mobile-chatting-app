#pragma once

#include <string>

namespace logger {

// Log user activity to file and console
void log_activity(const std::string& action, int userId, const std::string& details = "");

} // namespace logger
