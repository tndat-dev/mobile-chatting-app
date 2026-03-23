# Run From Scratch

Runbook nay gom cac lenh da dung de khoi dong lai toan bo flow tu dau, theo huong app Android ket noi vao backend deployment tren Kubernetes.

## 1. Kiem tra backend K8s

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl get pods -l app=chat-server -o wide
microk8s kubectl get pods -l app=postgres -o wide
microk8s kubectl get statefulset dev-postgres || true
microk8s kubectl logs -l app=chat-server --tail=80
microk8s kubectl get svc -A
```

Ky vong:
- Pod `chat-server` o trang thai `Running`
- Pod `postgres` o trang thai `Running`
- Service `dev-chat-server-service` expose cong `8080`

## 1.5. Build Docker image va deploy backend len K8s

Flow da duoc verify tren may nay theo huong local MicroK8s registry. Day la cach nen dung neu muon chay tu dau that su.

### Cach A. Khuyen dung tren may nay: build image va push vao local registry cua MicroK8s

Build image backend:

```bash
cd /home/tndat/Downloads/mobile-chatting-app/server-cpp
docker build -t chat-server:runbook-verify .
```

Tag va push vao local registry:

```bash
docker tag chat-server:runbook-verify localhost:32000/chat-server:runbook-verify
docker push localhost:32000/chat-server:runbook-verify
```

Cap nhat deployment de dung image vua push:

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl set image deployment/dev-chat-server chat-server=localhost:32000/chat-server:runbook-verify
microk8s kubectl rollout status deployment/dev-chat-server --timeout=240s
```

Kiem tra image dang chay:

```bash
microk8s kubectl get deployment dev-chat-server -o jsonpath='{.spec.template.spec.containers[0].image}'
echo
```

### Cach B. Apply lai overlay dev

Neu chi muon apply lai manifest dev overlay:

```bash
cd /home/tndat/Downloads/mobile-chatting-app/server-cpp
microk8s kubectl apply -k k8s/overlays/dev
microk8s kubectl rollout status deployment/dev-chat-server --timeout=240s
```

Neu muon force rollout khi image khong doi:

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl rollout restart deployment/dev-chat-server
microk8s kubectl rollout status deployment/dev-chat-server --timeout=240s
```

Kiem tra pod sau deploy:

```bash
microk8s kubectl get pods -l app=chat-server -o wide
microk8s kubectl logs -l app=chat-server --tail=80
```

### Luu y quan trong

- Image public `tuandat309/chat-server:v1.0.0` hien tai khong reliable tren may nay. Pod moi co the bi `ImagePullBackOff` voi loi `unexpected media type text/html`.
- Vi vay, de chay chac chan tren MicroK8s local, uu tien local registry `localhost:32000` nhu Cach A.

## 1.6. Deploy PostgreSQL bang StatefulSet (recommended)

PostgreSQL giờ được deploy chính thức bằng StatefulSet (tra file: `server-cpp/k8s/base/postgres-statefulset.yaml`):

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl apply -k server-cpp/k8s/overlays/dev
microk8s kubectl get pods -l app=postgres -w
```

Nếu cần clean slate (xóa StatefulSet & data cũ):

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl delete statefulset dev-postgres --ignore-not-found
microk8s kubectl delete pvc dev-postgres-pvc --ignore-not-found
sudo rm -rf /var/snap/microk8s/common/default-storage/default-dev-postgres-pvc*
microk8s kubectl apply -k server-cpp/k8s/overlays/dev
```

Apply schema từ ConfigMap:

```bash
cat > /tmp/apply-schema.yaml << 'EOF'
apiVersion: v1
kind: Pod
metadata:
  name: postgres-init
spec:
  containers:
  - name: postgres-client
    image: postgres:15
    command: ["psql"]
    args: ["-h", "dev-postgres-service", "-U", "chat_app_user", "-d", "chat_app_dev", "-f", "/schema.sql"]
    env:
    - name: PGPASSWORD
      value: "chat_app_password"
    volumeMounts:
    - name: schema-volume
      mountPath: /schema.sql
      subPath: schema.sql
  volumes:
  - name: schema-volume
    configMap:
      name: dev-postgres-schema-config
  restartPolicy: Never
EOF

microk8s kubectl delete pod postgres-init --ignore-not-found
microk8s kubectl apply -f /tmp/apply-schema.yaml
microk8s kubectl get pod postgres-init
```

Don pod chat-server cu bi loi pull image (neu con):

```bash
microk8s kubectl get pods -l app=chat-server
microk8s kubectl get pods -l app=chat-server | awk '/ImagePullBackOff|ErrImagePull/{print $1}' | xargs -r microk8s kubectl delete pod
```

## 2. Giai phong cong 30080 neu dang co tien trinh local chiem

NodePort `30080` la cach on dinh tren may nay (tranh loi TLS khi `kubectl port-forward`):

```bash
cd /home/tndat/Downloads/mobile-chatting-app
ss -ltnp | grep ':30080' || true
```

## 3. Expose backend qua NodePort 30080

```bash
cd /home/tndat/Downloads/mobile-chatting-app
microk8s kubectl patch svc dev-chat-server-service -p '{"spec":{"type":"NodePort","ports":[{"port":8080,"protocol":"TCP","targetPort":8080,"nodePort":30080}]}}'
microk8s kubectl get svc dev-chat-server-service -o wide
```

Neu can quay ve cach cu (port-forward), co the dung:

```bash
microk8s kubectl port-forward -n default svc/dev-chat-server-service 8080:8080
```

## 4. Smoke test backend qua localhost:30080

```bash
cd /home/tndat/Downloads/mobile-chatting-app
printf '1
0
' | server-cpp/build/test_client 127.0.0.1 30080
```

Ky vong:
- `Connected to server 127.0.0.1:30080`
- Response `type=240`

## 5. Build app Android

```bash
cd /home/tndat/Downloads/mobile-chatting-app
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
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
- Port: `30080`

Giai thich:
- Tu emulator, `10.0.2.2` tro ve may host
- Tren host, `30080` la NodePort map vao `svc/dev-chat-server-service:8080`

Luong ket noi thuc te:

```text
Android app -> 10.0.2.2:30080 -> NodePort service -> dev-chat-server-service -> chat-server pod
```

### Xac nhan da verify thanh cong

- App da ket noi thanh cong vao backend K8s qua `10.0.2.2:8080`
- Dang ky user moi `runuser313` da thanh cong va app da di vao man hinh chinh
- Neu login bang tai khoan seed that bai, uu tien dang ky user moi thay vi tin vao thong tin cu trong tai lieu

## 10. Lenh kiem tra nhanh khi gap loi

Kiem tra ai dang giu cong 30080:

```bash
ss -ltnp | grep ':30080' || true
```

Kiem tra service NodePort:

```bash
microk8s kubectl get svc dev-chat-server-service -o wide
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