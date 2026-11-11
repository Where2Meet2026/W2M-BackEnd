-- 시연 후 정리: 시연용 리뷰·설명·리뷰어 삭제
DELETE FROM reviews WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'demo_seed_%@w2m-demo.local');
DELETE FROM place_descriptions WHERE place_id IN ('1963225278', '1447732810', '1419437580', '1201355052', '1246535644');
DELETE FROM users WHERE email LIKE 'demo_seed_%@w2m-demo.local';
