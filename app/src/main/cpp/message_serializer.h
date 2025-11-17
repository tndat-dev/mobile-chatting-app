#ifndef MESSAGE_SERIALIZER_H
#define MESSAGE_SERIALIZER_H

#include <string>
#include <vector>
#include <map>
#include <cstdint>

class MessageSerializer {
public:
    MessageSerializer();

    // Serialize different message types
    std::string serializeLoginRequest(const std::string& username, const std::string& password);
    std::string serializeRegisterRequest(const std::string& username, const std::string& password, const std::string& email);
    std::string serializeChatMessage(uint32_t recipientId, const std::string& message);
    std::string serializeGroupMessage(uint32_t groupId, const std::string& message);
    std::string serializeFriendRequest(uint32_t targetUserId);
    std::string serializeCreateGroup(const std::string& groupName, const std::vector<uint32_t>& memberIds);

    // Deserialize messages
    bool deserializeLoginResponse(const std::string& data, uint32_t& userId, std::string& token, bool& success);
    bool deserializeChatMessage(const std::string& data, uint32_t& senderId, std::string& message, uint64_t& timestamp);
    bool deserializeFriendsList(const std::string& data, std::vector<std::pair<uint32_t, std::string>>& friends);
    bool deserializeGroupMessage(const std::string& data, uint32_t& groupId, uint32_t& senderId, std::string& message);

    // Generic JSON-like serialization
    std::string serialize(const std::map<std::string, std::string>& data);
    std::map<std::string, std::string> deserialize(const std::string& data);

private:
    // Helper functions for binary serialization
    void writeUint32(std::vector<uint8_t>& buffer, uint32_t value);
    void writeUint64(std::vector<uint8_t>& buffer, uint64_t value);
    void writeString(std::vector<uint8_t>& buffer, const std::string& str);
    
    uint32_t readUint32(const uint8_t*& ptr);
    uint64_t readUint64(const uint8_t*& ptr);
    std::string readString(const uint8_t*& ptr, size_t maxLength);
};

#endif // MESSAGE_SERIALIZER_H
