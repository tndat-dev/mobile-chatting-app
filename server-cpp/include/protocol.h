#pragma once

#include <cstdint>
#include <cstddef>

namespace proto {

constexpr uint32_t MAGIC = 0x43484154; // "CHAT"
constexpr uint8_t VERSION = 1;
constexpr size_t HEADER_SIZE = 32;

enum MessageType : uint8_t {
  REGISTER = 0x01,
  LOGIN = 0x02,
  LOGOUT = 0x03,
  SEARCH_USER = 0x04,
  GET_ALL_USERS = 0x05,
  FRIEND_REQUEST = 0x10,
  FRIEND_ACCEPT = 0x11,
  FRIEND_DECLINE = 0x12,
  UNFRIEND = 0x13,
  GET_FRIENDS_LIST = 0x14,
  DIRECT_MESSAGE = 0x20,
  MESSAGE_RECEIVED = 0x21,
  TYPING_STATUS = 0x22,
  GET_CONVERSATION_HISTORY = 0x23,
  GET_GROUP_HISTORY = 0x35,
  GET_USER_GROUPS = 0x36,
  // Additional group actions
  ADD_GROUP_MEMBERS = 0x37,
  RENAME_GROUP = 0x38,
  SET_GROUP_NICKNAME = 0x39,
  DELETE_CONVERSATION = 0x24,
  CREATE_GROUP = 0x30,
  INVITE_TO_GROUP = 0x31,
  ACCEPT_GROUP_INVITE = 0x3A,
  DECLINE_GROUP_INVITE = 0x3B,
  REMOVE_FROM_GROUP = 0x32,
  LEAVE_GROUP = 0x33,
  GROUP_MESSAGE = 0x34,
  USER_ONLINE = 0x40,
  USER_OFFLINE = 0x41,
  SUCCESS = 0xF0,
  ERROR = 0xF1,
  HEARTBEAT = 0xFF
};

struct Header {
  uint32_t magic;      // BE
  uint8_t version;
  uint8_t type;
  uint32_t length;     // BE
  int64_t timestamp;   // seconds
  uint32_t userId;     // BE
  uint32_t checksum;   // BE
  uint8_t padding[32 - 4 - 1 - 1 - 4 - 8 - 4 - 4]{};
};

} // namespace proto
