#ifndef PROTOCOL_HANDLER_H
#define PROTOCOL_HANDLER_H

#include <string>
#include <vector>
#include <cstdint>

// Message types
enum class MessageType : uint8_t {
    // Authentication
    REGISTER = 0x01,
    LOGIN = 0x02,
    LOGOUT = 0x03,
    SEARCH_USER = 0x04,
    GET_ALL_USERS = 0x05,
    
    // Friend management
    FRIEND_REQUEST = 0x10,
    FRIEND_ACCEPT = 0x11,
    FRIEND_DECLINE = 0x12,
    UNFRIEND = 0x13,
    GET_FRIENDS_LIST = 0x14,
    
    // Direct messaging
    DIRECT_MESSAGE = 0x20,
    MESSAGE_RECEIVED = 0x21,
    TYPING_STATUS = 0x22,
    
    // Group chat
    CREATE_GROUP = 0x30,
    INVITE_TO_GROUP = 0x31,
    REMOVE_FROM_GROUP = 0x32,
    LEAVE_GROUP = 0x33,
    GROUP_MESSAGE = 0x34,
    
    // Status
    USER_ONLINE = 0x40,
    USER_OFFLINE = 0x41,
    
    // Responses
    SUCCESS = 0xF0,
    ERROR = 0xF1,
    
    // Keep alive
    HEARTBEAT = 0xFF
};

// Protocol header structure
struct ProtocolHeader {
    uint32_t magic;        // Magic number for validation
    uint8_t version;       // Protocol version
    MessageType type;      // Message type
    uint32_t length;       // Payload length
    uint64_t timestamp;    // Unix timestamp
    uint32_t userId;       // Sender ID
    uint32_t checksum;     // CRC32 checksum
} __attribute__((packed));

class ProtocolHandler {
public:
    ProtocolHandler();
    
    // Create messages
    std::vector<uint8_t> createMessage(MessageType type, const std::string& payload, uint32_t userId = 0);
    
    // Parse messages
    bool parseMessage(const uint8_t* data, size_t length, ProtocolHeader& header, std::string& payload);
    
    // Validation
    bool validateMessage(const uint8_t* data, size_t length);
    uint32_t calculateChecksum(const uint8_t* data, size_t length);

private:
    static constexpr uint32_t PROTOCOL_MAGIC = 0x43484154; // "CHAT"
    static constexpr uint8_t PROTOCOL_VERSION = 1;
};

#endif // PROTOCOL_HANDLER_H
