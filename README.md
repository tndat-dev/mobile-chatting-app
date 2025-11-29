# mobile-chatting-app
Hybrid mobile app

Change local.properties sdk path

/home/tndat/Android/Sdk/emulator/emulator -avd Medium_Phone_API_36.1 &

./gradlew assembleDebug

./gradlew installDebug && adb shell am start -n com.example.myapplication/.MainActivity


cd /home/tndat/Downloads/mobile-chatting-app/server-cpp && rm -rf build && mkdir build && cd build && cmake .. && make

adb uninstall com.example.myapplication 2>/dev/null; cd /home/tndat/Downloads/mobile-chatting-app && ./gradlew installDebug && adb shell am start -n com.example.myapplication/.ui.activity.LoginActivity


cd /home/tndat/Downloads/mobile-chatting-app/server-cpp/build && ./chat_server