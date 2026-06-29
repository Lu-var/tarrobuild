SET search_path TO auth;

INSERT INTO credentials (email, password_hash, role, user_id, created_at)
VALUES ('admin@tarrobuild.cl', '$2a$10$qx0FyIxvAA1ULbkhw2r6w.UvkvWKTIRcE4nOt2Lqosm2u3/Ar7LLS', 'ADMIN', 1, NOW());

INSERT INTO credentials (email, password_hash, role, user_id, created_at)
VALUES ('user@tarrobuild.cl', '$2a$10$NI8fscfSID5r7ksQGMGfnuMgF1rwuvUWg2sv.juDobqqervs07mPO', 'USER', 2, NOW());
