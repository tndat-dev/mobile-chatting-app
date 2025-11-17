cmake_minimum_required(VERSION 3.22.1)

project("chatapp")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# OpenSSL (you may need to add prebuilt OpenSSL or use a simpler crypto implementation)
# For now, we'll use a simplified version without OpenSSL dependency

# Add source files
add_library(chatapp SHARED
    native-lib.cpp
    socket_manager.cpp
    protocol_handler.cpp
    crypto_utils.cpp
    message_serializer.cpp
)

# Include directories
target_include_directories(chatapp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Link libraries
target_link_libraries(chatapp
    ${log-lib}
    ${android-lib}
)
