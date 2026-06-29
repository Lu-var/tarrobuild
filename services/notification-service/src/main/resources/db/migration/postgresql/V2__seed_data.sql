SET search_path TO notification;

INSERT INTO notification_logs (user_id, type, content, status, timestamp) VALUES
(1, 'ESTIMATE_READY',        'Your budget estimate for build #1 is ready.', 'INFO', NOW()),
(1, 'COMPATIBILITY_WARNING', 'Build #3 has incompatible components: CPU socket LGA1700 does not match motherboard socket AM5.', 'WARNING', NOW()),
(2, 'RECOMMENDATION_READY', '3 upgrade suggestions available for your build.', 'SUCCESS', NOW());
