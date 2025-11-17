#include "logger.h"
#include <fstream>
#include <iostream>
#include <chrono>
#include <ctime>

namespace logger {

void log_activity(const std::string& action, int userId, const std::string& details) {
  auto now = std::chrono::system_clock::now();
  auto time_t_now = std::chrono::system_clock::to_time_t(now);
  char timestamp[64];
  std::strftime(timestamp, sizeof(timestamp), "%Y-%m-%d %H:%M:%S", std::localtime(&time_t_now));
  
  std::ofstream logfile("/tmp/chat_activity.log", std::ios::app);
  if (logfile) {
    logfile << "[" << timestamp << "] userId=" << userId 
            << " action=" << action;
    if (!details.empty()) {
      logfile << " details=" << details;
    }
    logfile << std::endl;
  }
  
  std::cout << "[LOG] userId=" << userId << " action=" << action;
  if (!details.empty()) {
    std::cout << " details=" << details;
  }
  std::cout << std::endl;
}

} // namespace logger
