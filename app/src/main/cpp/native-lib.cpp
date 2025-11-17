#include <jni.h>
#include <string>
#include <memory>
#include <map>
#include <sstream>
#include <iomanip>
#include <android/log.h>
#include "socket_manager.h"
#include "protocol_handler.h"
#include "crypto_utils.h"
#include "message_serializer.h"

#define LOG_TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global instances
static std::unique_ptr<SocketManager> g_socketManager;
static std::unique_ptr<ProtocolHandler> g_protocolHandler;
static std::unique_ptr<CryptoUtils> g_cryptoUtils;
static std::unique_ptr<MessageSerializer> g_serializer;
static JavaVM* g_jvm = nullptr;
static jobject g_callbackObject = nullptr;

// Helper function to convert jstring to std::string
std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string str(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return str;
}

// Helper function to convert std::string to jstring
jstring stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// Callback to Java when data is received
void onDataReceived(const char* data, size_t length) {
    LOGD("onDataReceived called with %zu bytes", length);
    
    if (!g_jvm || !g_callbackObject) {
        LOGE("JVM or callback object not set!");
        return;
    }

    JNIEnv* env;
    bool attached = false;
    
    int status = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        g_jvm->AttachCurrentThread(&env, nullptr);
        attached = true;
    }

    // Parse the message
    ProtocolHeader header;
    std::string payload;
    
    if (g_protocolHandler->parseMessage(reinterpret_cast<const uint8_t*>(data), length, header, payload)) {
        LOGD("Parsed message: type=0x%02X, payload=%s", header.type, payload.c_str());
        
        jclass callbackClass = env->GetObjectClass(g_callbackObject);
        jmethodID methodId = env->GetMethodID(callbackClass, "onMessageReceived", "(ILjava/lang/String;)V");
        
        if (methodId) {
            jstring jpayload = stringToJstring(env, payload);
            env->CallVoidMethod(g_callbackObject, methodId, static_cast<jint>(header.type), jpayload);
            
            // Check for exceptions
            if (env->ExceptionCheck()) {
                LOGE("Exception occurred in Java callback");
                env->ExceptionDescribe();
                env->ExceptionClear();
            } else {
                LOGD("Successfully called Java callback");
            }
            
            env->DeleteLocalRef(jpayload);
        } else {
            LOGE("Failed to find onMessageReceived method");
        }
        
        env->DeleteLocalRef(callbackClass);
    } else {
        LOGE("Failed to parse message");
    }

    if (attached) {
        g_jvm->DetachCurrentThread();
    }
}

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    LOGD("JNI_OnLoad called");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeInit(JNIEnv* env, jobject thiz) {
    LOGD("nativeInit called");
    g_socketManager = std::make_unique<SocketManager>();
    g_protocolHandler = std::make_unique<ProtocolHandler>();
    g_cryptoUtils = std::make_unique<CryptoUtils>();
    g_serializer = std::make_unique<MessageSerializer>();
}

JNIEXPORT void JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeDestroy(JNIEnv* env, jobject thiz) {
    LOGD("nativeDestroy called");
    
    if (g_callbackObject) {
        env->DeleteGlobalRef(g_callbackObject);
        g_callbackObject = nullptr;
    }
    
    g_socketManager.reset();
    g_protocolHandler.reset();
    g_cryptoUtils.reset();
    g_serializer.reset();
}

JNIEXPORT void JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeSetCallback(JNIEnv* env, jobject thiz, jobject callback) {
    LOGD("nativeSetCallback called");
    
    if (g_callbackObject) {
        env->DeleteGlobalRef(g_callbackObject);
    }
    
    g_callbackObject = env->NewGlobalRef(callback);
}

JNIEXPORT jboolean JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeConnect(JNIEnv* env, jobject thiz, jstring host, jint port) {
    if (!g_socketManager) {
        LOGE("SocketManager not initialized");
        return JNI_FALSE;
    }
    
    std::string hostStr = jstringToString(env, host);
    LOGD("nativeConnect: %s:%d", hostStr.c_str(), port);
    
    bool connected = g_socketManager->connect(hostStr, port);
    
    if (connected) {
        // Start receive loop
        g_socketManager->startReceiveLoop(onDataReceived);
    }
    
    return connected ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeDisconnect(JNIEnv* env, jobject thiz) {
    LOGD("nativeDisconnect called");
    
    if (g_socketManager) {
        g_socketManager->disconnect();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeIsConnected(JNIEnv* env, jobject thiz) {
    if (!g_socketManager) {
        return JNI_FALSE;
    }
    
    return g_socketManager->isConnected() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeSendMessage(JNIEnv* env, jobject thiz, jint messageType, jstring payload, jint userId) {
    if (!g_socketManager || !g_protocolHandler) {
        LOGE("Managers not initialized");
        return JNI_FALSE;
    }
    
    std::string payloadStr = jstringToString(env, payload);
    
    auto message = g_protocolHandler->createMessage(
        static_cast<MessageType>(messageType),
        payloadStr,
        userId
    );
    
    bool sent = g_socketManager->sendData(
        reinterpret_cast<const char*>(message.data()),
        message.size()
    );
    
    return sent ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeEncrypt(JNIEnv* env, jobject thiz, jstring data, jstring key) {
    if (!g_cryptoUtils) {
        LOGE("CryptoUtils not initialized");
        return nullptr;
    }
    
    std::string dataStr = jstringToString(env, data);
    std::string keyStr = jstringToString(env, key);
    
    std::vector<uint8_t> dataVec(dataStr.begin(), dataStr.end());
    auto encrypted = g_cryptoUtils->encryptAES(dataVec, keyStr);
    
    // Convert to hex string for easy transmission
    std::stringstream ss;
    for (uint8_t byte : encrypted) {
        ss << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(byte);
    }
    
    return stringToJstring(env, ss.str());
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeDecrypt(JNIEnv* env, jobject thiz, jstring encryptedHex, jstring key) {
    if (!g_cryptoUtils) {
        LOGE("CryptoUtils not initialized");
        return nullptr;
    }
    
    std::string hexStr = jstringToString(env, encryptedHex);
    std::string keyStr = jstringToString(env, key);
    
    // Convert hex string back to bytes
    std::vector<uint8_t> encrypted;
    for (size_t i = 0; i < hexStr.length(); i += 2) {
        std::string byteString = hexStr.substr(i, 2);
        uint8_t byte = static_cast<uint8_t>(std::strtol(byteString.c_str(), nullptr, 16));
        encrypted.push_back(byte);
    }
    
    auto decrypted = g_cryptoUtils->decryptAES(encrypted, keyStr);
    
    std::string result(decrypted.begin(), decrypted.end());
    return stringToJstring(env, result);
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeSha256(JNIEnv* env, jobject thiz, jstring data) {
    if (!g_cryptoUtils) {
        LOGE("CryptoUtils not initialized");
        return nullptr;
    }
    
    std::string dataStr = jstringToString(env, data);
    std::string hash = g_cryptoUtils->sha256(dataStr);
    
    return stringToJstring(env, hash);
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeSerializeLogin(JNIEnv* env, jobject thiz, jstring username, jstring password) {
    if (!g_serializer) {
        LOGE("Serializer not initialized");
        return nullptr;
    }
    
    std::string usernameStr = jstringToString(env, username);
    std::string passwordStr = jstringToString(env, password);
    
    std::string serialized = g_serializer->serializeLoginRequest(usernameStr, passwordStr);
    return stringToJstring(env, serialized);
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeSerializeRegister(JNIEnv* env, jobject thiz, jstring username, jstring password, jstring email) {
    if (!g_serializer) {
        LOGE("Serializer not initialized");
        return nullptr;
    }
    
    std::string usernameStr = jstringToString(env, username);
    std::string passwordStr = jstringToString(env, password);
    std::string emailStr = jstringToString(env, email);
    
    std::string serialized = g_serializer->serializeRegisterRequest(usernameStr, passwordStr, emailStr);
    return stringToJstring(env, serialized);
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_network_NetworkManager_nativeSerializeChatMessage(JNIEnv* env, jobject thiz, jint recipientId, jstring message) {
    if (!g_serializer) {
        LOGE("Serializer not initialized");
        return nullptr;
    }
    
    std::string messageStr = jstringToString(env, message);
    std::string serialized = g_serializer->serializeChatMessage(recipientId, messageStr);
    
    return stringToJstring(env, serialized);
}

} // extern "C"
