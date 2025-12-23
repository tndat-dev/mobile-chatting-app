#include "message_serializer.h"
#include <sstream>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "MessageSerializer"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

MessageSerializer::MessageSerializer() {
    LOGD("MessageSerializer created");
}

std::string MessageSerializer::serializeLoginRequest(const std::string& username, const std::string& password) {
    std::map<std::string, std::string> data;
    data["username"] = username;
    data["password"] = password;
    return serialize(data);
}

std::string MessageSerializer::serializeRegisterRequest(const std::string& username, const std::string& password, const std::string& email) {
    std::map<std::string, std::string> data;
    data["username"] = username;
    data["password"] = password;
    data["email"] = email;
    return serialize(data);
}

std::string MessageSerializer::serializeChatMessage(uint32_t recipientId, const std::string& message) {
    std::map<std::string, std::string> data;
    data["recipientId"] = std::to_string(recipientId);
    data["message"] = message;
    return serialize(data);
}

std::string MessageSerializer::serializeGroupMessage(uint32_t groupId, const std::string& message) {
    std::map<std::string, std::string> data;
    data["groupId"] = std::to_string(groupId);
    data["message"] = message;
    return serialize(data);
}

std::string MessageSerializer::serializeFriendRequest(uint32_t targetUserId) {
    std::map<std::string, std::string> data;
    data["targetUserId"] = std::to_string(targetUserId);
    return serialize(data);
}

std::string MessageSerializer::serializeCreateGroup(const std::string& groupName, const std::vector<uint32_t>& memberIds) {
    std::stringstream members;
    for (size_t i = 0; i < memberIds.size(); i++) {
        if (i > 0) members << ",";
        members << memberIds[i];
    }
    
    std::map<std::string, std::string> data;
    data["groupName"] = groupName;
    data["memberIds"] = members.str();
    return serialize(data);
}

bool MessageSerializer::deserializeLoginResponse(const std::string& data, uint32_t& userId, std::string& token, bool& success) {
    auto map = deserialize(data);
    
    if (map.find("success") != map.end()) {
        success = (map["success"] == "true" || map["success"] == "1");
    } else {
        return false;
    }
    
    if (success) {
        if (map.find("userId") != map.end()) {
            userId = std::stoul(map["userId"]);
        } else {
            return false;
        }
        
        if (map.find("token") != map.end()) {
            token = map["token"];
        } else {
            return false;
        }
    }
    
    return true;
}

bool MessageSerializer::deserializeChatMessage(const std::string& data, uint32_t& senderId, std::string& message, uint64_t& timestamp) {
    auto map = deserialize(data);
    
    if (map.find("senderId") == map.end() || map.find("message") == map.end()) {
        return false;
    }
    
    senderId = std::stoul(map["senderId"]);
    message = map["message"];
    
    if (map.find("timestamp") != map.end()) {
        timestamp = std::stoull(map["timestamp"]);
    } else {
        timestamp = 0;
    }
    
    return true;
}

bool MessageSerializer::deserializeFriendsList(const std::string& data, std::vector<std::pair<uint32_t, std::string>>& friends) {
    auto map = deserialize(data);
    
    if (map.find("count") == map.end()) {
        return false;
    }
    
    int count = std::stoi(map["count"]);
    friends.clear();
    
    for (int i = 0; i < count; i++) {
        std::string idKey = "id" + std::to_string(i);
        std::string nameKey = "name" + std::to_string(i);
        
        if (map.find(idKey) != map.end() && map.find(nameKey) != map.end()) {
            uint32_t id = std::stoul(map[idKey]);
            std::string name = map[nameKey];
            friends.push_back({id, name});
        }
    }
    
    return true;
}

bool MessageSerializer::deserializeGroupMessage(const std::string& data, uint32_t& groupId, uint32_t& senderId, std::string& message) {
    auto map = deserialize(data);
    
    if (map.find("groupId") == map.end() || map.find("senderId") == map.end() || map.find("message") == map.end()) {
        return false;
    }
    
    groupId = std::stoul(map["groupId"]);
    senderId = std::stoul(map["senderId"]);
    message = map["message"];
    
    return true;
}

std::string MessageSerializer::serialize(const std::map<std::string, std::string>& data) {
    // Simple key=value serialization format
    std::stringstream ss;
    bool first = true;
    
    for (const auto& pair : data) {
        if (!first) ss << "&";
        
        // Escape special characters
        std::string key = pair.first;
        std::string value = pair.second;
        
        // Simple URL-like encoding
        ss << key << "=" << value;
        first = false;
    }
    
    return ss.str();
}

std::map<std::string, std::string> MessageSerializer::deserialize(const std::string& data) {
    std::map<std::string, std::string> result;
    std::stringstream ss(data);
    std::string pair;
    
    while (std::getline(ss, pair, '&')) {
        size_t pos = pair.find('=');
        if (pos != std::string::npos) {
            std::string key = pair.substr(0, pos);
            std::string value = pair.substr(pos + 1);
            result[key] = value;
        }
    }
    
    return result;
}

void MessageSerializer::writeUint32(std::vector<uint8_t>& buffer, uint32_t value) {
    buffer.push_back((value >> 24) & 0xFF);
    buffer.push_back((value >> 16) & 0xFF);
    buffer.push_back((value >> 8) & 0xFF);
    buffer.push_back(value & 0xFF);
}

void MessageSerializer::writeUint64(std::vector<uint8_t>& buffer, uint64_t value) {
    buffer.push_back((value >> 56) & 0xFF);
    buffer.push_back((value >> 48) & 0xFF);
    buffer.push_back((value >> 40) & 0xFF);
    buffer.push_back((value >> 32) & 0xFF);
    buffer.push_back((value >> 24) & 0xFF);
    buffer.push_back((value >> 16) & 0xFF);
    buffer.push_back((value >> 8) & 0xFF);
    buffer.push_back(value & 0xFF);
}

void MessageSerializer::writeString(std::vector<uint8_t>& buffer, const std::string& str) {
    writeUint32(buffer, str.size());
    buffer.insert(buffer.end(), str.begin(), str.end());
}

uint32_t MessageSerializer::readUint32(const uint8_t*& ptr) {
    uint32_t value = (static_cast<uint32_t>(ptr[0]) << 24) |
                     (static_cast<uint32_t>(ptr[1]) << 16) |
                     (static_cast<uint32_t>(ptr[2]) << 8) |
                     static_cast<uint32_t>(ptr[3]);
    ptr += 4;
    return value;
}

uint64_t MessageSerializer::readUint64(const uint8_t*& ptr) {
    uint64_t value = (static_cast<uint64_t>(ptr[0]) << 56) |
                     (static_cast<uint64_t>(ptr[1]) << 48) |
                     (static_cast<uint64_t>(ptr[2]) << 40) |
                     (static_cast<uint64_t>(ptr[3]) << 32) |
                     (static_cast<uint64_t>(ptr[4]) << 24) |
                     (static_cast<uint64_t>(ptr[5]) << 16) |
                     (static_cast<uint64_t>(ptr[6]) << 8) |
                     static_cast<uint64_t>(ptr[7]);
    ptr += 8;
    return value;
}

std::string MessageSerializer::readString(const uint8_t*& ptr, size_t maxLength) {
    uint32_t length = readUint32(ptr);
    if (length > maxLength) {
        length = maxLength;
    }
    std::string str(reinterpret_cast<const char*>(ptr), length);
    ptr += length;
    return str;
}
