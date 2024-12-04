import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class PasswordUtils {

    // Method to hash a password using PBKDF2
    public static String hashPassword(String password, String salt) {
        try {
            // Create a PBEKeySpec with the password, salt, and iterations
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128); // 128-bit hash
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password: " + e.getMessage());
        }
    }

    // Method to generate a random salt
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Method to verify if a password matches the stored hash
    public static boolean verifyPassword(String enteredPassword, String storedHash, String salt) {
        String hashedPassword = hashPassword(enteredPassword, salt);
        return hashedPassword.equals(storedHash);
    }
}