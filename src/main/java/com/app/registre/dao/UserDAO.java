package com.app.registre.dao;

import com.app.registre.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserDAO {

    public UserDAO() {
        // Ensure the users table exists (Database.createTables already handles this on init)
        Database.getInstance();
    }

    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection c = Database.getInstance().getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // table might not exist yet
        }
        return 0;
    }

    public boolean createUser(String username, String plainPassword) {
        String sql = "INSERT INTO users(username,password_hash, display_name, email) VALUES(?,?,?,?)";
        String hash = createPasswordHash(plainPassword);
        try (Connection c = Database.getInstance().getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, hash);
            p.setString(3, null);
            p.setString(4, null);
            p.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, display_name, email FROM users WHERE username = ?";
        try (Connection c = Database.getInstance().getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("display_name"),
                            rs.getString("email")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean validateUser(String username, String plainPassword) {
        User u = findByUsername(username);
        if (u == null) return false;
        String stored = u.getPasswordHash();
        if (stored == null) return false;
        // If stored value looks like legacy SHA-256 hex (64 hex chars), verify and migrate
        if (stored.matches("^[0-9a-fA-F]{64}$")) {
            String sha = legacyHash(plainPassword);
            if (sha.equalsIgnoreCase(stored)) {
                // rehash with PBKDF2 and update DB
                String newHash = createPasswordHash(plainPassword);
                updatePassword(username, plainPassword); // will store newHash
                return true;
            }
            return false;
        }

        // Otherwise, stored format is pbkdf2$iter$base64salt$base64hash
        return verifyPassword(plainPassword, stored);
    }

    public java.util.List<User> listAllUsers() {
        java.util.List<User> out = new java.util.ArrayList<>();
        String sql = "SELECT id, username, password_hash, display_name, email FROM users ORDER BY username";
        try (Connection c = Database.getInstance().getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                out.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("display_name"), rs.getString("email")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public boolean deleteByUsername(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection c = Database.getInstance().getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            int r = p.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePassword(String username, String newPlainPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        String hash = createPasswordHash(newPlainPassword);
        try (Connection c = Database.getInstance().getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, hash);
            p.setString(2, username);
            int r = p.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProfile(String username, String displayName, String email) {
        String sql = "UPDATE users SET display_name = ?, email = ? WHERE username = ?";
        try (Connection c = Database.getInstance().getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, displayName);
            p.setString(2, email);
            p.setString(3, username);
            int r = p.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // --- Password hashing using PBKDF2WithHmacSHA256 ---
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITER = 65536;
    private static final int KEY_LEN = 256; // bits
    private static final int SALT_BYTES = 16;

    private String createPasswordHash(String plain) {
        if (plain == null) return "";
        try {
            byte[] salt = new byte[SALT_BYTES];
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
            byte[] hash = pbkdf2(plain.toCharArray(), salt, PBKDF2_ITER, KEY_LEN);
            String bSalt = Base64.getEncoder().encodeToString(salt);
            String bHash = Base64.getEncoder().encodeToString(hash);
            return String.format("pbkdf2$%d$%s$%s", PBKDF2_ITER, bSalt, bHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean verifyPassword(String plain, String stored) {
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) return false;
            if (!parts[0].equals("pbkdf2")) return false;
            int iter = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] hash = Base64.getDecoder().decode(parts[3]);
            byte[] computed = pbkdf2(plain.toCharArray(), salt, iter, hash.length * 8);
            return java.util.Arrays.equals(hash, computed);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGO);
        return skf.generateSecret(spec).getEncoded();
    }

    private String legacyHash(String plain) {
        if (plain == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            BigInteger bi = new BigInteger(1, digest);
            String hex = bi.toString(16);
            while (hex.length() < 64) hex = "0" + hex;
            return hex;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
