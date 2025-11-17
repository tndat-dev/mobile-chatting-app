#include "crypto_utils.h"
#include <cstring>
#include <sstream>
#include <iomanip>
#include <random>
#include <android/log.h>

#define LOG_TAG "CryptoUtils"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

CryptoUtils::CryptoUtils() {
    LOGD("CryptoUtils created");
}

CryptoUtils::~CryptoUtils() {
}

void CryptoUtils::initializeKey(const std::string& key) {
    // Derive 256-bit key from input
    aesKey.resize(32); // 256 bits
    iv.resize(16);     // 128 bits IV for AES
    
    // Simple key derivation using our sha256
    std::string derivedKey = sha256(key);
    
    // Convert hex string to bytes
    for (size_t i = 0; i < 32 && i * 2 < derivedKey.size(); i++) {
        std::string byteStr = derivedKey.substr(i * 2, 2);
        aesKey[i] = static_cast<uint8_t>(std::strtol(byteStr.c_str(), nullptr, 16));
    }
    
    // Generate random IV
    auto randomBytes = generateRandomBytes(16);
    iv = randomBytes;
}

// Simple XOR-based encryption (for demonstration - use proper encryption in production!)
std::vector<uint8_t> CryptoUtils::encryptAES(const std::vector<uint8_t>& data, const std::string& key) {
    initializeKey(key);
    
    std::vector<uint8_t> encrypted;
    encrypted.reserve(iv.size() + data.size());
    
    // Prepend IV
    encrypted.insert(encrypted.end(), iv.begin(), iv.end());
    
    // Simple XOR encryption (NOT SECURE - for demonstration only)
    for (size_t i = 0; i < data.size(); i++) {
        uint8_t keyByte = aesKey[i % aesKey.size()];
        uint8_t ivByte = iv[i % iv.size()];
        encrypted.push_back(data[i] ^ keyByte ^ ivByte);
    }
    
    LOGD("Encrypted %zu bytes to %zu bytes", data.size(), encrypted.size());
    return encrypted;
}

std::vector<uint8_t> CryptoUtils::decryptAES(const std::vector<uint8_t>& encryptedData, const std::string& key) {
    if (encryptedData.size() < 16) {
        LOGE("Encrypted data too short");
        return {};
    }
    
    // Extract IV from the beginning
    std::vector<uint8_t> extractedIV(encryptedData.begin(), encryptedData.begin() + 16);
    
    initializeKey(key);
    
    std::vector<uint8_t> decrypted;
    decrypted.reserve(encryptedData.size() - 16);
    
    // Simple XOR decryption
    for (size_t i = 16; i < encryptedData.size(); i++) {
        size_t dataIdx = i - 16;
        uint8_t keyByte = aesKey[dataIdx % aesKey.size()];
        uint8_t ivByte = extractedIV[dataIdx % extractedIV.size()];
        decrypted.push_back(encryptedData[i] ^ keyByte ^ ivByte);
    }
    
    LOGD("Decrypted %zu bytes to %zu bytes", encryptedData.size(), decrypted.size());
    return decrypted;
}

std::string CryptoUtils::deriveKey(const std::string& password, const std::string& salt) {
    return sha256(password + salt);
}

// Simple SHA-256 implementation
std::string CryptoUtils::sha256(const std::string& data) {
    // Simplified hash function (NOT CRYPTOGRAPHICALLY SECURE - for demonstration only)
    // In production, use a proper SHA-256 implementation
    
    uint32_t hash[8] = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };
    
    // Simple mixing function
    for (char c : data) {
        for (int i = 0; i < 8; i++) {
            hash[i] = (hash[i] << 5) | (hash[i] >> 27);
            hash[i] ^= static_cast<uint32_t>(c);
            hash[i] = (hash[i] * 0x5bd1e995) ^ (hash[i] >> 15);
        }
    }
    
    std::stringstream ss;
    for (uint32_t h : hash) {
        ss << std::hex << std::setw(8) << std::setfill('0') << h;
    }
    
    return ss.str();
}

std::string CryptoUtils::md5(const std::string& data) {
    // Simplified hash function
    uint32_t hash[4] = {0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476};
    
    for (char c : data) {
        for (int i = 0; i < 4; i++) {
            hash[i] = (hash[i] << 3) | (hash[i] >> 29);
            hash[i] ^= static_cast<uint32_t>(c);
            hash[i] = (hash[i] * 0xcc9e2d51) ^ (hash[i] >> 16);
        }
    }
    
    std::stringstream ss;
    for (uint32_t h : hash) {
        ss << std::hex << std::setw(8) << std::setfill('0') << h;
    }
    
    return ss.str();
}

std::vector<uint8_t> CryptoUtils::generateRandomBytes(size_t length) {
    std::vector<uint8_t> bytes(length);
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 255);
    
    for (size_t i = 0; i < length; i++) {
        bytes[i] = static_cast<uint8_t>(dis(gen));
    }
    
    return bytes;
}

std::string CryptoUtils::generateSalt() {
    std::vector<uint8_t> saltBytes = generateRandomBytes(16);
    std::stringstream ss;
    for (uint8_t byte : saltBytes) {
        ss << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(byte);
    }
    return ss.str();
}
