// Minimal test client for the chat_server protocol
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include <cstring>
#include <iostream>
#include <string>
#include "../include/protocol.h"
#include "../include/network_utils.h"

using namespace std;

bool connect_to_server(const char* host, uint16_t port, int& out_fd) {
  int fd = socket(AF_INET, SOCK_STREAM, 0);
  if (fd < 0) return false;
  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_port = htons(port);
  if (inet_pton(AF_INET, host, &addr.sin_addr) <= 0) {
    close(fd);
    return false;
  }
  if (connect(fd, (sockaddr*)&addr, sizeof(addr)) < 0) {
    close(fd);
    return false;
  }
  out_fd = fd;
  return true;
}

bool send_header_and_payload(int fd, uint8_t type, const string& payload, uint32_t userId) {
  char header[proto::HEADER_SIZE];
  network::write_header(header, type, static_cast<uint32_t>(payload.size()), userId);
  if (!network::send_all(fd, header, sizeof(header))) return false;
  if (!payload.empty()) {
    if (!network::send_all(fd, payload.data(), payload.size())) return false;
  }
  return true;
}

bool recv_response(int fd, uint8_t& out_type, string& out_payload) {
  char header[proto::HEADER_SIZE];
  ssize_t r = network::recv_all(fd, header, sizeof(header));
  if (r <= 0) return false;
  // parse length (big-endian at offset 6)
  uint32_t length_be;
  std::memcpy(&length_be, header + 6, 4);
  uint32_t length = ntohl(length_be);
  out_type = static_cast<uint8_t>(header[5]);
  out_payload.clear();
  if (length > 0) {
    out_payload.resize(length);
    ssize_t rr = network::recv_all(fd, &out_payload[0], length);
    if (rr <= 0) return false;
  }
  return true;
}

int main(int argc, char** argv) {
  const char* host = "127.0.0.1";
  uint16_t port = 8080;
  if (argc >= 2) host = argv[1];
  if (argc >= 3) port = static_cast<uint16_t>(atoi(argv[2]));

  int fd;
  if (!connect_to_server(host, port, fd)) {
    cerr << "Failed to connect to " << host << ":" << port << "\n";
    return 2;
  }
  cout << "Connected to server " << host << ":" << port << "\n";

  // Simple interactive loop: send GET_USER_GROUPS for a provided userId
  cout << "Enter a userId to request GET_USER_GROUPS (or 0 to exit): ";
  uint32_t userId;
  while (cin >> userId) {
    if (userId == 0) break;
    // Send GET_USER_GROUPS with empty payload; put userId in header
    if (!send_header_and_payload(fd, proto::GET_USER_GROUPS, string(), userId)) {
      cerr << "Failed to send request\n";
      break;
    }
    uint8_t resp_type;
    string resp_payload;
    if (!recv_response(fd, resp_type, resp_payload)) {
      cerr << "Failed to receive response\n";
      break;
    }
    cout << "Response type=" << (int)resp_type << " payload_len=" << resp_payload.size() << "\n";
    if (!resp_payload.empty()) cout << "Payload:\n" << resp_payload << "\n";
    cout << "Enter next userId (0 to exit): ";
  }

  close(fd);
  cout << "Exited\n";
  return 0;
}
