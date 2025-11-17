#pragma once

#include <string>
#include <netinet/in.h>

namespace handler {

// Handle a single client connection
void handle_client(int client_fd, sockaddr_in addr);

} // namespace handler
