#include "socket_manager.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <android/log.h>
#include <cstring>
#include <vector>

#define LOG_TAG "SocketManager"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

SocketManager::SocketManager() : sockfd(-1), connected(false), receiving(false) {
    LOGD("SocketManager created");
}

SocketManager::~SocketManager() {
    disconnect();
}

bool SocketManager::connect(const std::string& host, int port) {
    std::lock_guard<std::mutex> lock(socketMutex);
    
    if (connected) {
        LOGD("Already connected");
        return true;
    }

    // Create socket
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        LOGE("Failed to create socket");
        return false;
    }

    // Setup server address
    struct sockaddr_in serverAddr;
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(port);
    
    if (inet_pton(AF_INET, host.c_str(), &serverAddr.sin_addr) <= 0) {
        LOGE("Invalid address: %s", host.c_str());
        close(sockfd);
        sockfd = -1;
        return false;
    }

    // Connect to server
    if (::connect(sockfd, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) < 0) {
        LOGE("Connection failed to %s:%d", host.c_str(), port);
        close(sockfd);
        sockfd = -1;
        return false;
    }

    connected = true;
    LOGD("Connected to %s:%d", host.c_str(), port);
    return true;
}

void SocketManager::disconnect() {
    stopReceiveLoop();
    
    std::lock_guard<std::mutex> lock(socketMutex);
    
    if (sockfd >= 0) {
        close(sockfd);
        sockfd = -1;
    }
    
    connected = false;
    LOGD("Disconnected");
}

bool SocketManager::isConnected() const {
    return connected;
}

bool SocketManager::sendData(const char* data, size_t length) {
    std::lock_guard<std::mutex> lock(socketMutex);
    
    if (!connected || sockfd < 0) {
        LOGE("Not connected");
        return false;
    }

    ssize_t totalSent = 0;
    while (totalSent < length) {
        ssize_t sent = send(sockfd, data + totalSent, length - totalSent, MSG_NOSIGNAL);
        if (sent < 0) {
            LOGE("Send failed");
            connected = false;
            return false;
        }
        totalSent += sent;
    }

    LOGD("Sent %zu bytes", length);
    return true;
}

int SocketManager::receiveData(char* buffer, size_t maxLength) {
    std::lock_guard<std::mutex> lock(socketMutex);
    
    if (!connected || sockfd < 0) {
        LOGE("Not connected");
        return -1;
    }

    ssize_t received = recv(sockfd, buffer, maxLength, 0);
    if (received <= 0) {
        if (received == 0) {
            LOGD("Connection closed by peer");
        } else {
            LOGE("Receive failed");
        }
        connected = false;
        return -1;
    }

    LOGD("Received %zd bytes", received);
    return static_cast<int>(received);
}

void SocketManager::startReceiveLoop(std::function<void(const char*, size_t)> callback) {
    if (receiving) {
        LOGD("Receive loop already running");
        return;
    }

    receiving = true;
    receiveThread = std::thread(&SocketManager::receiveLoop, this, callback);
    LOGD("Started receive loop");
}

void SocketManager::stopReceiveLoop() {
    if (!receiving) {
        return;
    }

    receiving = false;
    
    if (receiveThread.joinable()) {
        receiveThread.join();
    }
    
    LOGD("Stopped receive loop");
}

void SocketManager::receiveLoop(std::function<void(const char*, size_t)> callback) {
    const size_t BUFFER_SIZE = 8192;
    const size_t HEADER_SIZE = 32; // ProtocolHeader size
    char buffer[BUFFER_SIZE];
    std::vector<uint8_t> messageBuffer; // Accumulate incomplete messages

    while (receiving && connected) {
        // Direct recv without locking to avoid deadlock with sendData
        ssize_t received = recv(sockfd, buffer, BUFFER_SIZE, 0);
        
        if (received > 0) {
            LOGD("Received %zd bytes in loop", received);
            
            // Append received data to message buffer
            messageBuffer.insert(messageBuffer.end(), buffer, buffer + received);
            
            // Process all complete messages in buffer
            while (messageBuffer.size() >= HEADER_SIZE) {
                // Read payload length from header (offset 6: 4 bytes magic + 1 version + 1 type = 6)
                // Length is in network byte order (big-endian), convert to host byte order
                uint32_t payloadLength;
                memcpy(&payloadLength, messageBuffer.data() + 6, sizeof(uint32_t));
                payloadLength = ntohl(payloadLength);
                
                size_t totalMessageSize = HEADER_SIZE + payloadLength;
                
                // Check if we have complete message
                if (messageBuffer.size() >= totalMessageSize) {
                    // Extract and process complete message
                    callback(reinterpret_cast<const char*>(messageBuffer.data()), totalMessageSize);
                    
                    // Remove processed message from buffer
                    messageBuffer.erase(messageBuffer.begin(), messageBuffer.begin() + totalMessageSize);
                    LOGD("Processed message of %zu bytes, %zu bytes remaining in buffer", totalMessageSize, messageBuffer.size());
                } else {
                    // Incomplete message, wait for more data
                    LOGD("Incomplete message: have %zu bytes, need %zu bytes", messageBuffer.size(), totalMessageSize);
                    break;
                }
            }
        } else if (received == 0) {
            LOGD("Connection closed by peer in receive loop");
            connected = false;
            break;
        } else {
            LOGE("Receive error in loop: %d", errno);
            connected = false;
            break;
        }
    }

    LOGD("Receive loop ended");
}
