-- Admin user (password: admin123)
INSERT INTO credentials (email, password_hash, role, user_id, created_at)
SELECT 'admin@tarrobuild.cl', '$2a$10$qx0FyIxvAA1ULbkhw2r6w.UvkvWKTIRcE4nOt2Lqosm2u3/Ar7LLS', 'ADMIN', 1, NOW()
    WHERE NOT EXISTS (SELECT 1 FROM credentials WHERE email = 'admin@tarrobuild.cl');

-- Test user (password: test123)
INSERT INTO credentials (email, password_hash, role, user_id, created_at)
SELECT 'user@tarrobuild.cl', '$2a$10$NI8fscfSID5r7ksQGMGfnuMgF1rwuvUWg2sv.juDobqqervs07mPO', 'USER', 2, NOW()
    WHERE NOT EXISTS (SELECT 1 FROM credentials WHERE email = 'user@tarrobuild.cl');