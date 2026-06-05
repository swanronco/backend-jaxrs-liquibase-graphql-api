package com.example.graphql.resolver;

import com.example.dto.LogoutResponse;
import com.example.model.User;
import com.example.user.UserRepository;
import com.example.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.concurrent.*;

public class UserMutationResolver {
    private final UserRepository userRepository = new UserRepository();
    private static final ConcurrentMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    static {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                UserMutationResolver::purgeExpiredTokens,
                1, 1, TimeUnit.DAYS
        );
    }

    public Map<String, Object> login(String identifier, String password) {
        User user = this.userRepository.findByUsernameOrEmail(identifier);
        if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return Map.of(
                "token", JwtUtil.generateToken(user.getUsername()),
                "user", user
        );
    }

    public LogoutResponse logout(String token) {
        try {
            long expiration = JwtUtil.getExpiration(token);
            blacklistedTokens.put(token, expiration);
            return new LogoutResponse(true, "Déconnexion réussie");
        } catch (Exception e) {
            return new LogoutResponse(false, "Échec de la déconnexion : " + e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    private static void purgeExpiredTokens() {
        long now = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
