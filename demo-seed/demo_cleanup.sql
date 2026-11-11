-- 시연 후 정리: 시연용 리뷰·설명·리뷰어 삭제
DELETE FROM reviews WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'demo_seed_%@w2m-demo.local');
DELETE FROM place_descriptions WHERE place_id IN ('27494588', '1941860892', '476059630', '1963225278', '1447732810', '922091993', '1419437580', '1201355052', '1260273250');
DELETE FROM users WHERE email LIKE 'demo_seed_%@w2m-demo.local';
