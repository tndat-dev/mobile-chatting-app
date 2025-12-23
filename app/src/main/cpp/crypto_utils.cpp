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

// Standard SHA-256 implementation (self-contained)
// This implementation follows the FIPS 180-4 specification and returns
// the lowercase hex representation of the 32-byte digest.
static inline uint32_t rotr(uint32_t x, uint32_t n) { return (x >> n) | (x << (32 - n)); }

std::string CryptoUtils::sha256(const std::string& data) {
    static const uint32_t K[64] = {
        0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
        0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
        0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
        0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
        0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
        0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
        0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
        0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
    };

    // initial hash values
    uint32_t H[8] = {
        0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,
        0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19
    };

    // Pre-processing (padding)
    uint64_t bitlen = static_cast<uint64_t>(data.size()) * 8;
    // append 0x80, then pad with zeros until length mod 512 == 448
    std::string padded = data;
    padded.push_back(char(0x80));
    while ((padded.size() * 8) % 512 != 448) padded.push_back(char(0));
    // append 64-bit big-endian length
    for (int i = 7; i >= 0; --i) {
        padded.push_back(static_cast<char>((bitlen >> (i * 8)) & 0xFF));
    }

    // Process the message in successive 512-bit chunks
    size_t chunks = padded.size() * 8 / 512;
    for (size_t chunk = 0; chunk < chunks; ++chunk) {
        uint32_t W[64];
        // Prepare the message schedule
        const unsigned char* chunk_ptr = reinterpret_cast<const unsigned char*>(padded.data() + chunk * 64);
        for (int t = 0; t < 16; ++t) {
            W[t] = (static_cast<uint32_t>(chunk_ptr[t * 4]) << 24) |
                   (static_cast<uint32_t>(chunk_ptr[t * 4 + 1]) << 16) |
                   (static_cast<uint32_t>(chunk_ptr[t * 4 + 2]) << 8) |
                   (static_cast<uint32_t>(chunk_ptr[t * 4 + 3]));
        }
        for (int t = 16; t < 64; ++t) {
            uint32_t s0 = rotr(W[t-15], 7) ^ rotr(W[t-15], 18) ^ (W[t-15] >> 3);
            uint32_t s1 = rotr(W[t-2], 17) ^ rotr(W[t-2], 19) ^ (W[t-2] >> 10);
            W[t] = W[t-16] + s0 + W[t-7] + s1;
        }

        // Initialize working variables
        uint32_t a = H[0];
        uint32_t b = H[1];
        uint32_t c = H[2];
        uint32_t d = H[3];
        uint32_t e = H[4];
        uint32_t f = H[5];
        uint32_t g = H[6];
        uint32_t h = H[7];

        for (int t = 0; t < 64; ++t) {
            uint32_t S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
            uint32_t ch = (e & f) ^ ((~e) & g);
            uint32_t temp1 = h + S1 + ch + K[t] + W[t];
            uint32_t S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
            uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
            uint32_t temp2 = S0 + maj;

            h = g;
            g = f;
            f = e;
            e = d + temp1;
            d = c;
            c = b;
            b = a;
            a = temp1 + temp2;
        }

        H[0] += a;
        H[1] += b;
        H[2] += c;
        H[3] += d;
        H[4] += e;
        H[5] += f;
        H[6] += g;
        H[7] += h;
    }

    // Produce the final hash value (big-endian)
    std::stringstream ss;
    ss << std::hex << std::setfill('0');
    for (int i = 0; i < 8; ++i) {
        ss << std::setw(8) << (H[i]);
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
