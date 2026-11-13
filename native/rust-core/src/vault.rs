use crate::{CoreError, Result};
use aes_gcm::aead::{Aead, KeyInit};
use aes_gcm::{Aes256Gcm, Key, Nonce};
use zeroize::Zeroize;

/// Memory-scrubbed AES-256-GCM vault for provider credentials.
/// The 32-byte key material is unwrapped by Android Keystore on the Kotlin side
/// and zeroized in Rust immediately after sealing/unsealing.
pub struct SecretVault {
    key_bytes: [u8; 32],
}

impl SecretVault {
    pub fn from_raw_key(key_bytes: [u8; 32]) -> Self {
        Self { key_bytes }
    }

    pub fn seal(&self, nonce_12: &[u8; 12], plaintext: &[u8]) -> Result<Vec<u8>> {
        let key = Key::<Aes256Gcm>::from_slice(&self.key_bytes);
        let cipher = Aes256Gcm::new(key);
        cipher
            .encrypt(Nonce::from_slice(nonce_12), plaintext)
            .map_err(|e| CoreError::Vault(e.to_string()))
    }

    pub fn unseal(&self, nonce_12: &[u8; 12], ciphertext: &[u8]) -> Result<Vec<u8>> {
        let key = Key::<Aes256Gcm>::from_slice(&self.key_bytes);
        let cipher = Aes256Gcm::new(key);
        cipher
            .decrypt(Nonce::from_slice(nonce_12), ciphertext)
            .map_err(|e| CoreError::Vault(e.to_string()))
    }
}

impl Drop for SecretVault {
    fn drop(&mut self) {
        self.key_bytes.zeroize();
    }
}
