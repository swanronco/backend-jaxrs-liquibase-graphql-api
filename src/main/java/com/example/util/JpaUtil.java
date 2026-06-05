package com.example.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;


public class JpaUtil {
    private static EntityManagerFactory emf;

    public static void init(java.util.Map<String, Object> properties) {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        emf = Persistence.createEntityManagerFactory("default", properties);
    }

    public static EntityManager getEntityManager() {
        if (emf == null) {
            java.util.Map<String, Object> props = new java.util.HashMap<>();
            String dbUrl = System.getenv("DB_URL");
            if (dbUrl != null) props.put("jakarta.persistence.jdbc.url", dbUrl);
            String dbUser = System.getenv("DB_USER");
            if (dbUser != null) props.put("jakarta.persistence.jdbc.user", dbUser);
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbPassword != null) props.put("jakarta.persistence.jdbc.password", dbPassword);
            emf = Persistence.createEntityManagerFactory("default", props);
        }
        return emf.createEntityManager();
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}