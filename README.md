# Mobile Chatting App (K8s Ready) 🚀

This project is a high-performance chatting application featuring a **C++ Backend**, **PostgreSQL Database**, and **Android (Kotlin) Frontend**. It is fully containerized and ready for deployment on **Kubernetes (MicroK8s)** following CKA best practices.

## 🏗 System Architecture

- **Backend**: C++ server using Alpine Linux for ⚡ performance and minimal image size (~4.5MB).
- **Database**: PostgreSQL with persistent storage (PVC) and automatic schema initialization.
- **Orchestration**: Kubernetes using **Kustomize** to manage `dev`, `staging`, and `prod` environments.
- **Frontend**: Native Android app built with Kotlin.

---

## ☸️ Kubernetes Deployment (MicroK8s)

The following steps will deploy the **Database** and **Backend** server in the correct order.

### 1. Prerequisites
- [MicroK8s](https://microk8s.io/) installed and active.
- `alias k='microk8s kubectl'` configured.

### 2. One-Click Deployment (Dev Environment)
From the root of the project:

```bash
cd server-cpp
# Deploy Database, Service, PVC, Secret, and Backend
sudo microk8s kubectl apply -k k8s/overlays/dev
```

### 3. Order of Initialization
1. **PostgreSQL**: Deploys first. It uses `schema.sql` (mounted via ConfigMap) to automatically create tables and seed data (`alice`, `dat`).
2. **Backend**: Deploys after. It connects to the database using environmental variables provided by Kubernetes Secrets.
    - Startup includes PostgreSQL retry logic to tolerate transient DNS/service readiness issues.
    - Optional env vars:
       - `DB_CONNECT_RETRIES` (default: `10`)
       - `DB_CONNECT_RETRY_DELAY_SECONDS` (default: `2`)

### 4. Verification
Check if all pods are running:
```bash
k get pods -w
```

Check the backend logs to confirm database connectivity:
```bash
k logs -l app=chat-server --tail=50
```
*Expected output: `[PERSISTENCE] Connected to PostgreSQL successfully`*

---

## 📱 Mobile App (Kotlin)

### 1. Requirements
- Android Studio installed.
- Emulator or physical device connected via `adb`.

### 2. Backend Connection
The mobile app connects to the K8s backend via **Port Forwarding** (since the Service is `ClusterIP` by default for security).

```bash
# In a separate terminal, forward the traffic
sudo microk8s kubectl port-forward service/dev-chat-server-service 8080:8080
```

### 3. Build and Run
1. Open the `/app` folder in Android Studio.
2. Build the project and run on your device.
3. Login with test accounts:
   - **User:** `alice` / **Pass:** `password`
   - **User:** `dat` / **Pass:** `password`

---

## 🛠 Manual Development & Docker

### Docker Build (Included Build Steps)
The `Dockerfile` in `server-cpp/` uses a **multi-stage build** which automatically compiles the C++ server using CMake within an Alpine environment.

**Build the image:**
```bash
cd server-cpp
docker build -t your-username/chat-server:latest .
```

**Build natively (no Docker):**
```bash
cd server-cpp
mkdir build && cd build
cmake ..
make -j$(nproc)
./chat_server
```

---

## 📂 Project Structure
```text
.
├── app/                # Android Kotlin Source
├── server-cpp/         # C++ Backend Source
│   ├── k8s/            # Kubernetes Manifests (Base & Overlays)
│   ├── src/            # C++ Source Code
│   ├── Dockerfile      # Multi-stage Alpine Build
│   └── schema.sql      # Database Schema & Seed Data
└── README.md           # This file
```

---

## 🚀 CI/CD (GitHub Actions)

The project includes automated workflows for continuous integration and delivery:

![Android CI](https://github.com/tndat-dev/mobile-chatting-app/actions/workflows/android.yml/badge.svg?branch=main)
![C++ Server CI](https://github.com/tndat-dev/mobile-chatting-app/actions/workflows/cpp-server.yml/badge.svg?branch=main)
![Kubernetes Lint](https://github.com/tndat-dev/mobile-chatting-app/actions/workflows/k8s-lint.yml/badge.svg?branch=main)

- **Android CI**: Builds the Android app and runs unit tests on every push to `main`.
- **C++ Server CI**: Builds the C++ server using CMake and verifies compilation.
- **Kubernetes Lint**: Uses `kube-linter` to check Kubernetes manifests for best practices.
- **Docker Build**: Runs on tag pushes (`v*`) or manual dispatch (`workflow_dispatch`).

You can find the workflow definitions in `.github/workflows/`.

---

---
**Author:** tndat-dev
