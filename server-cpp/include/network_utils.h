#pragma once

#include <string>
#include <cstdint>
#include <sys/types.h>

namespace network {

// Receive exactly len bytes from socket
ssize_t recv_all(int fd, void* buf, size_t len);

// Send exactly len bytes to socket
bool send_all(int fd, const void* buf, size_t len);

// Write protocol header
void write_header(char out[32], uint8_t type, uint32_t length, uint32_t userId);

// Send message with header and payload
bool send_message(int fd, uint8_t type, const std::string& payload, uint32_t userId = 0);

} // namespace network
