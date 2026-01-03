# Mobile Chatting App - Kubernetes Deployment

Hybrid mobile chat application with microservices architecture deployed on Kubernetes.

## 📋 Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Deployment](#deployment)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [CKA Practice Topics](#cka-practice-topics)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Ingress Controller                 │
│              (chat.yourdomain.com)                   │
└──────────────────────┬──────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │    Chat Server Service       │
        │      (LoadBalancer)          │
        └──────────────┬──────────────┘
                       │
        ┌──────────────┴──────────────┐
        │   Chat Server Pods (3x)     │
        │   - C++ Application         │
        │   - HPA enabled             │
        │   - Resource limits         │
        └──────────────┬──────────────┘
                       │
        ┌──────────────┴──────────────┐
        │   PostgreSQL Service        │
        │   - StatefulSet             │
        │   - Persistent Volume       │
        └─────────────────────────────┘
```

### Components

- **Android App**: Kotlin-based mobile client
- **Chat Server**: C++ backend with socket programming
- **Database**: PostgreSQL 15 with persistent storage
- **Ingress**: NGINX ingress controller
- **Monitoring**: Prometheus + Grafana (optional)

## 🔧 Prerequisites

### Required Software

- **Kubernetes Cluster** (v1.24+)
  - Minikube (for local development)
  - or Production cluster (GKE, EKS, AKS, etc.)
- **kubectl** (v1.24+)
- **Docker** (v20.10+)
- **Docker Registry** (Docker Hub, GCR, ECR, etc.)

### Optional Tools

- **Helm** (v3.0+) - for installing nginx-ingress
- **k9s** - Kubernetes CLI dashboard
- **kubectx/kubens** - context and namespace switching

### Installation Commands

```bash
# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install Minikube (for local testing)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start Minikube with sufficient resources
minikube start --cpus=4 --memory=8192 --driver=docker

# Enable ingress addon
minikube addons enable ingress

# Install Helm (optional)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

## 🚀 Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/tndat-dev/mobile-chatting-app.git
cd mobile-chatting-app
```

### 2. Configure Docker Registry

Update the `Makefile` with your Docker registry:

```bash
# Edit Makefile
vim Makefile

# Change this line:
DOCKER_REGISTRY ?= your-registry

# To your actual registry, e.g.:
DOCKER_REGISTRY ?= docker.io/yourusername
```

### 3. Update Secrets (Important!)

Generate secure credentials for production:

```bash
# Generate base64 encoded password
echo -n 'your-secure-password' | base64

# Edit secrets file
vim kubernetes/secrets.yaml

# Replace default passwords with secure ones
```

### 4. Deploy Everything

```bash
# Build, push, and deploy all components
make deploy-all

# Or step by step:
make build-server        # Build Docker image
make push-server         # Push to registry
make deploy-postgres     # Deploy database
make init-database       # Initialize schema
make deploy-chatserver   # Deploy application
make deploy-ingress      # Deploy ingress
```

### 5. Check Status

```bash
# View all resources
make status

# Watch pods coming up
kubectl get pods -n chat-app -w

# Check services
kubectl get svc -n chat-app
```

## 📦 Deployment

### Step-by-Step Deployment

#### 1. Create Namespace

```bash
kubectl apply -f kubernetes/namespace.yaml
```

#### 2. Deploy Secrets and ConfigMaps

```bash
kubectl apply -f kubernetes/secrets.yaml
kubectl apply -f kubernetes/configmap.yaml
```

#### 3. Deploy PostgreSQL

```bash
# Create persistent storage
kubectl apply -f kubernetes/postgres-pv.yaml

# Deploy PostgreSQL
kubectl apply -f kubernetes/postgres-deployment.yaml

# Wait for ready
kubectl wait --for=condition=ready pod -l app=postgres -n chat-app --timeout=300s
```

#### 4. Initialize Database

```bash
# Run migration job
kubectl apply -f kubernetes/db-init-job.yaml

# Check job status
kubectl get jobs -n chat-app
kubectl logs job/db-migration -n chat-app
```

#### 5. Build and Deploy Chat Server

```bash
# Build image
docker build -t your-registry/chat-server:latest ./server-cpp

# Push to registry
docker push your-registry/chat-server:latest

# Update deployment with your image
vim kubernetes/chatserver-deployment.yaml
# Change: image: your-registry/chat-server:latest

# Deploy
kubectl apply -f kubernetes/chatserver-deployment.yaml

# Wait for ready
kubectl wait --for=condition=ready pod -l app=chat-server -n chat-app --timeout=300s
```

#### 6. Deploy Network Policies (Optional)

```bash
kubectl apply -f kubernetes/network-policy.yaml
```

#### 7. Deploy Ingress

```bash
# Update domain in ingress.yaml
vim kubernetes/ingress.yaml

# Deploy
kubectl apply -f kubernetes/ingress.yaml
```

## ⚙️ Configuration

### Environment Variables

ConfigMap (`kubernetes/configmap.yaml`):
- `DB_HOST`: PostgreSQL service hostname
- `DB_PORT`: PostgreSQL port (5432)
- `DB_NAME`: Database name
- `SERVER_PORT`: Chat server port (8080)
- `MAX_CONNECTIONS`: Maximum concurrent connections
- `LOG_LEVEL`: Application log level

Secrets (`kubernetes/secrets.yaml`):
- `postgres_user`: Database username
- `postgres_password`: Database password
- `jwt_secret`: JWT signing key

### Resource Limits

Default resource allocation:

**PostgreSQL:**
- Requests: 256Mi RAM, 250m CPU
- Limits: 512Mi RAM, 500m CPU

**Chat Server:**
- Requests: 256Mi RAM, 250m CPU
- Limits: 512Mi RAM, 500m CPU

Adjust in deployment files as needed.

### Horizontal Pod Autoscaling

Chat server automatically scales between 2-10 replicas based on:
- CPU utilization: 70%
- Memory utilization: 80%

```bash
# Check HPA status
kubectl get hpa -n chat-app

# Manually scale
make scale-chatserver REPLICAS=5
```

## 📊 Monitoring

### Check Logs

```bash
# PostgreSQL logs
make logs-postgres

# Chat server logs
make logs-chatserver

# Or directly with kubectl
kubectl logs -f deployment/chat-server -n chat-app
```

### Database Access

```bash
# Open psql shell
make shell-postgres

# Or manually
kubectl exec -it deployment/postgres -n chat-app -- psql -U chat_app_user -d chat_app

# Test connection
make test-connection
```

### Port Forwarding

```bash
# Forward PostgreSQL to localhost:5432
make port-forward-postgres

# Forward chat server to localhost:8080
make port-forward-chatserver

# Test with curl
curl http://localhost:8080/health
```

### View Events

```bash
# Recent events
make events

# Or directly
kubectl get events -n chat-app --sort-by='.lastTimestamp'
```

### Describe Resources

```bash
# Describe all pods
make describe-pods

# Describe specific resource
kubectl describe pod <pod-name> -n chat-app
```

## 💻 Development

### Local Development Setup

```bash
# 1. Setup Android development
export ANDROID_HOME=/path/to/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/emulator

# 2. Start emulator
$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_36.1 &

# 3. Build APK
./gradlew assembleDebug

# 4. Install and run
./gradlew installDebug && \
  adb shell am start -n com.example.myapplication/.ui.activity.LoginActivity
```

### Building C++ Server Locally

```bash
cd server-cpp
mkdir build && cd build
cmake ..
make -j$(nproc)
./chat_server
```

### Database Commands

```bash
# Connect to local PostgreSQL
psql "host=localhost port=5432 dbname=chat_app user=chat_app_user password=chat_app_password"

# List tables
\dt

# View schema
\d+ users
```

### Uninstall App

```bash
adb uninstall com.example.myapplication
```

## 🔧 Troubleshooting

### Common Issues

#### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n chat-app

# Describe pod for events
kubectl describe pod <pod-name> -n chat-app

# Check logs
kubectl logs <pod-name> -n chat-app
```

#### Database Connection Issues

```bash
# Verify PostgreSQL is running
kubectl get pods -l app=postgres -n chat-app

# Check service
kubectl get svc postgres-service -n chat-app

# Test connection from pod
kubectl run -it --rm debug --image=postgres:15-alpine --restart=Never -n chat-app -- \
  psql -h postgres-service -U chat_app_user -d chat_app
```

#### ImagePullBackOff

```bash
# Check if image exists
docker pull your-registry/chat-server:latest

# Verify image name in deployment
kubectl get deployment chat-server -n chat-app -o yaml | grep image:

# Check image pull secrets if using private registry
kubectl get secrets -n chat-app
```

#### Persistent Volume Issues

```bash
# Check PV and PVC status
kubectl get pv,pvc -n chat-app

# Describe PVC
kubectl describe pvc postgres-pvc -n chat-app

# For Minikube, ensure storage directory exists
minikube ssh "sudo mkdir -p /mnt/data/postgres"
```

### Reset Everything

```bash
# Delete all resources
make delete

# Clean Docker images
make clean

# Restart from scratch
make deploy-all
```

## 📚 CKA Practice Topics

This deployment covers the following CKA exam topics:

### Core Concepts
- ✅ Creating and configuring namespaces
- ✅ Understanding Kubernetes API primitives
- ✅ Managing pods, deployments, services

### Configuration
- ✅ ConfigMaps and Secrets
- ✅ Resource requirements and limits
- ✅ Environment variables

### Multi-Container Pods
- ✅ Init containers (wait-for-postgres)

### Observability
- ✅ Liveness and readiness probes
- ✅ Container logging
- ✅ Monitoring with kubectl

### Services & Networking
- ✅ ClusterIP services
- ✅ LoadBalancer services
- ✅ Ingress controllers
- ✅ Network policies

### Storage
- ✅ PersistentVolumes (PV)
- ✅ PersistentVolumeClaims (PVC)
- ✅ Storage classes
- ✅ Volume mounts

### Workloads
- ✅ Deployments with rolling updates
- ✅ Jobs for database migrations
- ✅ StatefulSets concepts
- ✅ Horizontal Pod Autoscaler (HPA)

### Cluster Architecture
- ✅ Node management
- ✅ Resource quotas
- ✅ Label selectors

### Troubleshooting
- ✅ Debugging pod issues
- ✅ Viewing logs
- ✅ Executing commands in containers
- ✅ Network troubleshooting

## 📝 Makefile Commands

```bash
make help                    # Show all available commands
make build-server           # Build Docker image
make push-server            # Push image to registry
make deploy-all             # Deploy everything
make status                 # Check deployment status
make logs-chatserver        # View server logs
make logs-postgres          # View database logs
make shell-postgres         # Open PostgreSQL shell
make test-connection        # Test DB connection
make scale-chatserver       # Scale server replicas
make rollback-chatserver    # Rollback to previous version
make delete                 # Delete all resources
make clean                  # Clean everything
```

## 🔐 Security Considerations

### Production Recommendations

1. **Change Default Passwords**: Update all secrets with strong passwords
2. **Enable TLS**: Configure SSL/TLS certificates for ingress
3. **Network Policies**: Enable network policies to restrict traffic
4. **RBAC**: Implement role-based access control
5. **Pod Security**: Use pod security policies/standards
6. **Image Scanning**: Scan Docker images for vulnerabilities
7. **Secrets Management**: Consider using external secret managers (Vault, Sealed Secrets)

### Example: Generate Secure Secrets

```bash
# Generate random password
openssl rand -base64 32

# Encode for Kubernetes
echo -n 'your-password' | base64

# Update secrets.yaml with new values
```

## 📄 License

MIT License - See LICENSE file for details

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📞 Support

- Create an issue on GitHub
- Check troubleshooting section
- Review Kubernetes documentation

## 🎓 Learning Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [CKA Exam Curriculum](https://github.com/cncf/curriculum)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)

---

**Note**: This is a learning project for CKA certification practice. For production use, implement additional security measures and high availability configurations.
