cmake_minimum_required(VERSION 3.15)
project(chat_server_cpp)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include directories
include_directories(include)

# Find PostgreSQL
if(UNIX AND NOT APPLE)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(POSTGRES REQUIRED libpq)
    include_directories(${POSTGRES_INCLUDE_DIRS})
    link_directories(${POSTGRES_LIBRARY_DIRS})
    add_definitions(${POSTGRES_CFLAGS_OTHER})
else()
    find_package(PostgreSQL REQUIRED)
    include_directories(${PostgreSQL_INCLUDE_DIRS})
endif()

# Source files
set(SOURCES
    src/main.cpp
    src/network_utils.cpp
    src/utils.cpp
    src/logger.cpp
    src/persistence.cpp
    src/persistence_pg.cpp
    src/server_state.cpp
    src/message_handler.cpp
)

# Create executable
add_executable(chat_server ${SOURCES})

# pthread for Linux
if(UNIX)
  find_package(Threads REQUIRED)
  target_link_libraries(chat_server PRIVATE 
    Threads::Threads
    ${POSTGRES_LIBRARIES}
    ${PostgreSQL_LIBRARIES}
  )
endif()

# Minimal test client for protocol testing (no Postgres dependency)
add_executable(test_client tools/test_client.cpp src/network_utils.cpp)
if(UNIX)
  target_link_libraries(test_client PRIVATE Threads::Threads)
endif()
