#include "network_utils.h"
#include "protocol.h"
#include <sys/socket.h>
#include <arpa/inet.h>
#include <cstring>
#include <ctime>
#include <iostream>

namespace network {

ssize_t recv_all(int fd, void* buf, size_t len) {
  size_t got = 0;
  char* p = static_cast<char*>(buf);
  while (got < len) {
    ssize_t r = ::recv(fd, p + got, len - got, 0);
    if (r <= 0) return r;
    got += r;
  }
  return got;
}

bool send_all(int fd, const void* buf, size_t len) {
  size_t sent = 0;
  const char* p = static_cast<const char*>(buf);
  while (sent < len) {
    ssize_t s = ::send(fd, p + sent, len - sent, 0);
    if (s <= 0) return false;
    sent += s;
  }
  return true;
}

void write_header(char out[32], uint8_t type, uint32_t length, uint32_t userId) {
  std::memset(out, 0, proto::HEADER_SIZE);
  
  uint32_t magic_be = htonl(proto::MAGIC);
  std::memcpy(out + 0, &magic_be, 4);
  
  out[4] = proto::VERSION;
  out[5] = type;
  
  uint32_t length_be = htonl(length);
  std::memcpy(out + 6, &length_be, 4);
  
  uint64_t ts = static_cast<uint64_t>(time(nullptr));
  uint32_t ts_high = htonl((uint32_t)(ts >> 32));
  uint32_t ts_low = htonl((uint32_t)(ts & 0xFFFFFFFF));
  std::memcpy(out + 10, &ts_high, 4);
  std::memcpy(out + 14, &ts_low, 4);
  
  uint32_t userId_be = htonl(userId);
  std::memcpy(out + 18, &userId_be, 4);
  
  uint32_t checksum_be = htonl(0);
  std::memcpy(out + 22, &checksum_be, 4);
}

bool send_message(int fd, uint8_t type, const std::string& payload, uint32_t userId) {
  std::cout << "[C++ SERVER] Sending response: type=" << (int)type << " (" 
            << (type == proto::SUCCESS ? "SUCCESS" : type == proto::ERROR ? "ERROR" : "OTHER")
            << "), userId=" << userId << ", payload_len=" << payload.size() << std::endl;
  if (!payload.empty()) {
    std::cout << "[C++ SERVER] Response payload: " << payload << std::endl;
  }
  
  char header[proto::HEADER_SIZE];
  write_header(header, type, static_cast<uint32_t>(payload.size()), userId);
  
  if (!send_all(fd, header, sizeof(header))) {
    std::cout << "[C++ SERVER] Failed to send header" << std::endl;
    return false;
  }
  
  if (!payload.empty()) {
    if (!send_all(fd, payload.data(), payload.size())) {
      std::cout << "[C++ SERVER] Failed to send payload" << std::endl;
      return false;
    }
  }
  
  std::cout << "[C++ SERVER] Response sent successfully" << std::endl;
  return true;
}

} // namespace network
