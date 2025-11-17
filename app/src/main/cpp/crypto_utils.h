#ifndef CRYPTO_UTILS_H
#define CRYPTO_UTILS_H

#include <string>
#include <vector>
#include <cstdint>

class CryptoUtils {
public:
    CryptoUtils();
    ~CryptoUtils();

    // AES-256 encryption/decryption
    std::vector<uint8_t> encryptAES(const std::vector<uint8_t>& data, const std::string& key);
    std::vector<uint8_t> decryptAES(const std::vector<uint8_t>& encryptedData, const std::string& key);

    // Key derivation
    std::string deriveKey(const std::string& password, const std::string& salt);

    // Hashing
    std::string sha256(const std::string& data);
    std::string md5(const std::string& data);

    // Random generation
    std::vector<uint8_t> generateRandomBytes(size_t length);
    std::string generateSalt();

private:
    void initializeKey(const std::string& key);
    std::vector<uint8_t> aesKey;
    std::vector<uint8_t> iv;
};

#endif // CRYPTO_UTILS_H
