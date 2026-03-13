# Run From Scratch

Runbook nay gom cac lenh da dung de khoi dong lai toan bo flow tu dau, theo huong app Android ket noi vao backend deployment tren Kubernetes.

## 1. Kiem tra backend K8s

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl get pods -l app=chat-server -o wide
microk8s kubectl logs -l app=chat-server --tail=80
microk8s kubectl get svc -A
```

Ky vong:
- Pod `chat-server` o trang thai `Running`
- Service `dev-chat-server-service` expose cong `8080`

## 1.5. Build Docker image va deploy backend len K8s

Neu can rebuild backend image truoc khi chay app, dung 1 trong 2 huong duoi day.

### Cach A. Build image moi, push registry, roi cap nhat deployment

Manifest hien tai dang tro vao image dang public:

```bash
tuandat309/chat-server:v1.0.0
```

Build image moi:

```bash
cd /home/tndat/Downloads/mobile-chatting-app/server-cpp
docker build -t tuandat309/chat-server:v1.0.1 .
```

Push image len registry:

```bash
docker push tuandat309/chat-server:v1.0.1
```

Cap nhat deployment de dung image moi:

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl set image deployment/dev-chat-server chat-server=tuandat309/chat-server:v1.0.1
microk8s kubectl rollout status deployment/dev-chat-server
```

Kiem tra image dang chay:

```bash
microk8s kubectl get deployment dev-chat-server -o jsonpath='{.spec.template.spec.containers[0].image}'
echo
```

### Cach B. Deploy lai overlay hien co

Neu khong doi image, chi can apply lai manifest dev overlay:

```bash
cd /home/tndat/Downloads/mobile-chatting-app/server-cpp
microk8s kubectl apply -k k8s/overlays/dev
microk8s kubectl rollout status deployment/dev-chat-server
```

Neu muon force rollout du image tag khong doi:

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl rollout restart deployment/dev-chat-server
microk8s kubectl rollout status deployment/dev-chat-server
```

Kiem tra pod sau deploy:

```bash
microk8s kubectl get pods -l app=chat-server -o wide
microk8s kubectl logs -l app=chat-server --tail=80
```

## 2. Giai phong cong 8080 neu dang co backend local chiem

Neu truoc do da chay `chat_server` local, dung no de danh `8080` cho `kubectl port-forward`:

```bash
cd /home/tndat/Downloads/mobile-chatting-app
ss -ltnp | grep ':8080' || true
pkill -f '/chat_server| chat_server' || true
ss -ltnp | grep ':8080' || true
```

## 3. Forward backend K8s ve may host

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl port-forward -n default svc/dev-chat-server-service 8080:8080
```

Lenh nay can duoc giu chay o 1 terminal rieng.

## 4. Smoke test backend qua localhost:8080

```bash
cd /home/tndat/Downloads/mobile-chatting-app
printf '1
0
' | server-cpp/build/test_client 127.0.0.1 8080
```

Ky vong:
- `Connected to server 127.0.0.1:8080`
- Response `type=240`

## 5. Build app Android

```bash
cd /home/tndat/Downloads/mobile-chatting-app
./gradlew :app:assembleDebug --no-daemon
```

APK tao ra tai:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## 6. Liet ke emulator co san

```bash
cd /home/tndat/Downloads/mobile-chatting-app
"$HOME/Android/Sdk/emulator/emulator" -list-avds
```

AVD da dung trong phien nay:

```bash
Medium_Phone_API_36.1
```

## 7. Bat emulator co giao dien

Neu emulator thuong bi crash voi GPU host, dung software renderer:

```bash
cd /home/tndat/Downloads/mobile-chatting-app
"$HOME/Android/Sdk/emulator/emulator" -avd Medium_Phone_API_36.1 -no-snapshot -no-boot-anim -no-audio -gpu swiftshader_indirect
```

Kiem tra ADB da thay emulator:

```bash
cd /home/tndat/Downloads/mobile-chatting-app
adb devices -l
```

## 8. Cai va mo app tren emulator

```bash
cd /home/tndat/Downloads/mobile-chatting-app
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.example.myapplication -c android.intent.category.LAUNCHER 1
```

## 9. Cau hinh trong app

Tai man hinh login:
- Host: `10.0.2.2`
- Port: `8080`

Giai thich:
- Tu emulator, `10.0.2.2` tro ve may host
- Tren host, `8080` dang duoc forward vao `svc/dev-chat-server-service`

Luong ket noi thuc te:

```text
Android app -> 10.0.2.2:8080 -> kubectl port-forward -> dev-chat-server-service -> chat-server pod
```

## 10. Lenh kiem tra nhanh khi gap loi

Kiem tra ai dang giu cong 8080:

```bash
ss -ltnp | grep ':8080' || true
```

Kiem tra port-forward con song:

```bash
pgrep -af 'kubectl port-forward.*dev-chat-server-service' || true
```

Kiem tra log backend:

```bash
microk8s kubectl logs -l app=chat-server --tail=80
```

Kiem tra thiet bi Android:

```bash
adb devices -l
```

## 11. Tuy chon: validate local Android day du hon

```bash
cd /home/tndat/Downloads/mobile-chatting-app
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

## 12. Tuy chon: build lai C++ backend sach tu dau

```bash
cd /home/tndat/Downloads/mobile-chatting-app
cmake -S server-cpp -B /tmp/chat-server-verify
cmake --build /tmp/chat-server-verify -j$(nproc)
```