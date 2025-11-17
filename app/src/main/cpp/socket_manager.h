#ifndef SOCKET_MANAGER_H
#define SOCKET_MANAGER_H

#include <string>
#include <functional>
#include <thread>
#include <atomic>
#include <mutex>

class SocketManager {
public:
    SocketManager();
    ~SocketManager();

    // Connection management
    bool connect(const std::string& host, int port);
    void disconnect();
    bool isConnected() const;

    // Send/Receive data
    bool sendData(const char* data, size_t length);
    int receiveData(char* buffer, size_t maxLength);

    // Async receive with callback
    void startReceiveLoop(std::function<void(const char*, size_t)> callback);
    void stopReceiveLoop();

private:
    int sockfd;
    std::atomic<bool> connected;
    std::atomic<bool> receiving;
    std::thread receiveThread;
    std::mutex socketMutex;

    void receiveLoop(std::function<void(const char*, size_t)> callback);
};

#endif // SOCKET_MANAGER_H
