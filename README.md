# Mobile Chatting App

A hybrid mobile chatting application built with Android (Kotlin) and a C++ backend server, featuring real-time messaging capabilities and PostgreSQL database integration.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Database Setup](#database-setup)
- [Development](#development)
- [License](#license)

## 🔍 Overview

This is a real-time mobile chatting application that combines an Android frontend with a high-performance C++ backend server. The application provides instant messaging capabilities with a focus on performance and scalability.

## ✨ Features

- Real-time messaging
- User authentication and login system
- PostgreSQL database integration
- Custom C++ chat server
- Android native UI with Kotlin
- Efficient message handling
- Server management scripts

## 🛠 Tech Stack

### Frontend
- **Language**: Kotlin (21.3%)
- **Platform**: Android
- **Build Tool**: Gradle

### Backend
- **Language**: C++ (25.6%)
- **Database**: PostgreSQL
- **Build Tool**: CMake

### Additional
- HTML (23.0%)
- Makefile (12.0%)
- C (10.9%)
- CMake (6.4%)

## 📁 Project Structure

```
mobile-chatting-app/
├── app/                      # Android application source code
├── server-cpp/               # C++ chat server implementation
├── database/                 # Database migrations and setup
│   └── migrations/
├── scripts/                  # Utility scripts for automation
├── gradle/                   # Gradle wrapper files
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts       # Gradle settings
├── local.properties          # Local SDK configuration
└── README.md
```

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Android Studio** (latest version)
- **Android SDK**
- **JDK 11** or higher
- **CMake** (for C++ compilation)
- **PostgreSQL** (version 12 or higher)
- **g++** or **clang** (C++ compiler)
- **Make**

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/tndat-dev/mobile-chatting-app.git
cd mobile-chatting-app
```

### 2. Configure Android SDK Path

Edit `local.properties` and set your Android SDK path:

```properties
sdk.dir=/path/to/your/Android/Sdk
```

For example:
```properties
sdk.dir=/home/tndat/Android/Sdk
```

### 3. Build the C++ Server

```bash
cd server-cpp
rm -rf build
mkdir build
cd build
cmake ..
make
```

## ⚙️ Configuration

### Database Configuration

The application uses PostgreSQL as its database. Configure your database connection with the following credentials:

- **Host**: localhost
- **Port**: 5432
- **Database**: chat_app
- **User**: chat_app_user
- **Password**: chat_app_password

### Database Setup Commands

Create the database and user:

```sql
CREATE DATABASE chat_app;
CREATE USER chat_app_user WITH PASSWORD 'chat_app_password';
GRANT ALL PRIVILEGES ON DATABASE chat_app TO chat_app_user;
```

Check tables:

```bash
psql "host=localhost port=5432 dbname=chat_app user=chat_app_user password=chat_app_password" -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public';"
```

## 🎮 Running the Application

### Start Android Emulator

```bash
/path/to/Android/Sdk/emulator/emulator -avd Medium_Phone_API_36.1 &
```

Or use the example path:
```bash
/home/tndat/Android/Sdk/emulator/emulator -avd Medium_Phone_API_36.1 &
```

### Build and Install the Android App

#### Option 1: Build and Install Separately
```bash
# Build the APK
./gradlew assembleDebug

# Install and launch
./gradlew installDebug && adb shell am start -n com.example.myapplication/.MainActivity
```

#### Option 2: Clean Install with Launch
```bash
adb uninstall com.example.myapplication 2>/dev/null; \
./gradlew installDebug && \
adb shell am start -n com.example.myapplication/.ui.activity.LoginActivity
```

### Start the C++ Chat Server

```bash
cd /path/to/mobile-chatting-app/server-cpp/build
./chat_server
```

Or using the full path:
```bash
cd /home/tndat/mobile-chatting-app/server-cpp/build && ./chat_server
```

### Stop the Chat Server

```bash
pkill -9 chat_server
```

## 🔧 Development

### Project Logs

The application generates several log files for debugging:

- `chat_server_run.log` - Chat server runtime logs
- `server_run.log` - General server logs
- `server.log` - Additional server information
- `server.pid` - Process ID of running server

### Clean Build

To perform a clean build of the C++ server:

```bash
cd /home/tndat/mobile-chatting-app/server-cpp
rm -rf build
mkdir build
cd build
cmake ..
make
```

### Android Build

To build the Android debug APK:

```bash
./gradlew assembleDebug
```

### Uninstall Previous Version

Before installing a new version:

```bash
adb uninstall com.example.myapplication
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**tndat-dev**

- GitHub: [@tndat-dev](https://github.com/tndat-dev)

## 🙏 Acknowledgments

- Thanks to all contributors who have helped with this project
- Special thanks to the Android and C++ communities for their excellent documentation

## 📞 Support

If you encounter any issues or have questions, please file an issue on the [GitHub repository](https://github.com/tndat-dev/mobile-chatting-app/issues).

---

**Note**: This is a development project. Make sure to update security credentials and configurations before deploying to production.
