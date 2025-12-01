# mobile-chatting-app
Hybrid mobile app

Change local.properties sdk path

/home/tndat/Android/Sdk/emulator/emulator -avd Medium_Phone_API_36.1 &

./gradlew assembleDebug

./gradlew installDebug && adb shell am start -n com.example.myapplication/.MainActivity


cd /home/tndat/mobile-chatting-app/server-cpp && rm -rf build && mkdir build && cd build && cmake .. && make

adb uninstall com.example.myapplication 2>/dev/null; cd /home/tndat/mobile-chatting-app && ./gradlew installDebug && adb shell am start -n com.example.myapplication/.ui.activity.LoginActivity

pkill -9 chat_server

cd /home/tndat/mobile-chatting-app/server-cpp/build && ./chat_server

psql "host=localhost port=5432 dbname=chat_app user=chat_app_user password=chat_app_password" -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public';"