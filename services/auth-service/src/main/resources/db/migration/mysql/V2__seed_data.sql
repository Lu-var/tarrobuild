-- Admin user (password: admin123)
INSERT INTO credentials (email, password_hash, role, user_id, created_at)
VALUES ('admin@tarrobuild.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 1, NOW());

-- Test user (password: test123)
INSERT INTO credentials (email, password_hash, role, user_id, created_at)
VALUES ('test@tarrobuild.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', 2, NOW());
