-- ===== 1) 시연용 리뷰어 5명 (이미 있으면 건너뜀) =====
INSERT IGNORE INTO users (created_at, updated_at, email, name, provider) VALUES (NOW(), NOW(), 'demo_seed_1@w2m-demo.local', '시연리뷰어1', 'LOCAL');
INSERT IGNORE INTO users (created_at, updated_at, email, name, provider) VALUES (NOW(), NOW(), 'demo_seed_2@w2m-demo.local', '시연리뷰어2', 'LOCAL');
INSERT IGNORE INTO users (created_at, updated_at, email, name, provider) VALUES (NOW(), NOW(), 'demo_seed_3@w2m-demo.local', '시연리뷰어3', 'LOCAL');
INSERT IGNORE INTO users (created_at, updated_at, email, name, provider) VALUES (NOW(), NOW(), 'demo_seed_4@w2m-demo.local', '시연리뷰어4', 'LOCAL');
INSERT IGNORE INTO users (created_at, updated_at, email, name, provider) VALUES (NOW(), NOW(), 'demo_seed_5@w2m-demo.local', '시연리뷰어5', 'LOCAL');

-- ===== 2) 카페별 리뷰 5개 (같은 장소·같은 사용자는 한 번만 들어가므로 다시 실행해도 안전) =====
-- 키하라
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1963225278', '키하라', '서울 용산구 효창동 3-118', 0, 4.5, '동네 분위기가 느껴지는 편안한 카페예요', NOW() - INTERVAL 2 DAY FROM users WHERE email = 'demo_seed_1@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1963225278', '키하라', '서울 용산구 효창동 3-118', 0, 4.0, '혼자 와서 책 읽기에도 좋았어요', NOW() - INTERVAL 4 DAY FROM users WHERE email = 'demo_seed_2@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1963225278', '키하라', '서울 용산구 효창동 3-118', 0, 5.0, '사장님이 친절하고 응대가 따뜻했어요', NOW() - INTERVAL 6 DAY FROM users WHERE email = 'demo_seed_3@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1963225278', '키하라', '서울 용산구 효창동 3-118', 0, 4.0, '자리가 아담해서 소수 인원이 모이기 좋아요', NOW() - INTERVAL 8 DAY FROM users WHERE email = 'demo_seed_4@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1963225278', '키하라', '서울 용산구 효창동 3-118', 0, 4.5, '음악이 잔잔해서 대화가 잘 들렸어요', NOW() - INTERVAL 10 DAY FROM users WHERE email = 'demo_seed_5@w2m-demo.local';
-- 스트라다로스터스 공덕점
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1447732810', '스트라다로스터스 공덕점', '서울 마포구 공덕동 242-86', 0, 5.0, '원두 향이 진해서 커피 좋아하는 친구들이 만족했어요', NOW() - INTERVAL 2 DAY FROM users WHERE email = 'demo_seed_1@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1447732810', '스트라다로스터스 공덕점', '서울 마포구 공덕동 242-86', 0, 4.5, '로스터리라 그런지 커피 맛에 신경 쓴 게 느껴졌어요', NOW() - INTERVAL 4 DAY FROM users WHERE email = 'demo_seed_2@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1447732810', '스트라다로스터스 공덕점', '서울 마포구 공덕동 242-86', 0, 4.0, '공간이 넓고 천장이 높아 개방감이 있어요', NOW() - INTERVAL 6 DAY FROM users WHERE email = 'demo_seed_3@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1447732810', '스트라다로스터스 공덕점', '서울 마포구 공덕동 242-86', 0, 4.5, '공덕역에서 가까워 약속 장소로 좋아요', NOW() - INTERVAL 8 DAY FROM users WHERE email = 'demo_seed_4@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1447732810', '스트라다로스터스 공덕점', '서울 마포구 공덕동 242-86', 0, 4.0, '점심 이후에는 자리가 금방 차요', NOW() - INTERVAL 10 DAY FROM users WHERE email = 'demo_seed_5@w2m-demo.local';
-- 영앤도터스
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1419437580', '영앤도터스', '서울 마포구 공덕동 475', 0, 4.5, '채광이 좋아서 낮에 가면 기분이 좋아져요', NOW() - INTERVAL 2 DAY FROM users WHERE email = 'demo_seed_1@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1419437580', '영앤도터스', '서울 마포구 공덕동 475', 0, 4.0, '테이블이 넓어서 노트북 작업하기에도 좋았어요', NOW() - INTERVAL 4 DAY FROM users WHERE email = 'demo_seed_2@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1419437580', '영앤도터스', '서울 마포구 공덕동 475', 0, 5.0, '직원분들이 밝고 친절하세요', NOW() - INTERVAL 6 DAY FROM users WHERE email = 'demo_seed_3@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1419437580', '영앤도터스', '서울 마포구 공덕동 475', 0, 4.5, '과하지 않은 인테리어라 편안했어요', NOW() - INTERVAL 8 DAY FROM users WHERE email = 'demo_seed_4@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1419437580', '영앤도터스', '서울 마포구 공덕동 475', 0, 4.0, '와이파이가 잘 터져서 작업하기 편했어요', NOW() - INTERVAL 10 DAY FROM users WHERE email = 'demo_seed_5@w2m-demo.local';
-- 스타벅스 공덕점
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1201355052', '스타벅스 공덕점', '서울 마포구 공덕동 254-8', 0, 4.0, '자리가 넓고 콘센트가 많아 모임이나 작업하기 편해요', NOW() - INTERVAL 2 DAY FROM users WHERE email = 'demo_seed_1@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1201355052', '스타벅스 공덕점', '서울 마포구 공덕동 254-8', 0, 4.5, '어디서든 익숙한 맛이라 실패가 없어요', NOW() - INTERVAL 4 DAY FROM users WHERE email = 'demo_seed_2@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1201355052', '스타벅스 공덕점', '서울 마포구 공덕동 254-8', 0, 4.0, '점심시간에는 줄이 길 수 있어요', NOW() - INTERVAL 6 DAY FROM users WHERE email = 'demo_seed_3@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1201355052', '스타벅스 공덕점', '서울 마포구 공덕동 254-8', 0, 4.5, '여럿이 앉을 큰 테이블이 있어서 좋았어요', NOW() - INTERVAL 8 DAY FROM users WHERE email = 'demo_seed_4@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1201355052', '스타벅스 공덕점', '서울 마포구 공덕동 254-8', 0, 4.0, '역과 가까워 길 찾기가 쉬워요', NOW() - INTERVAL 10 DAY FROM users WHERE email = 'demo_seed_5@w2m-demo.local';
-- 포멜로빈 공덕점
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1246535644', '포멜로빈 공덕점', '서울 마포구 공덕동 478', 0, 4.5, '커피 맛이 깔끔해서 아메리카노를 마시러 일부러 찾아왔어요', NOW() - INTERVAL 2 DAY FROM users WHERE email = 'demo_seed_1@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1246535644', '포멜로빈 공덕점', '서울 마포구 공덕동 478', 0, 4.0, '공덕역에서 가까워 약속 장소로 잡기 편해요', NOW() - INTERVAL 4 DAY FROM users WHERE email = 'demo_seed_2@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1246535644', '포멜로빈 공덕점', '서울 마포구 공덕동 478', 0, 4.5, '점심 이후에도 자리가 넉넉해서 오래 앉아 있기 좋았어요', NOW() - INTERVAL 6 DAY FROM users WHERE email = 'demo_seed_3@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1246535644', '포멜로빈 공덕점', '서울 마포구 공덕동 478', 0, 5.0, '바리스타분이 친절하고 원두 설명을 잘 해주셨어요', NOW() - INTERVAL 8 DAY FROM users WHERE email = 'demo_seed_4@w2m-demo.local';
INSERT IGNORE INTO reviews (user_id, place_id, place_name, place_address, meeting_id, rating, comment, created_at) SELECT id, '1246535644', '포멜로빈 공덕점', '서울 마포구 공덕동 478', 0, 4.0, '테이블이 넓어 노트북 작업이나 모임에 편했어요', NOW() - INTERVAL 10 DAY FROM users WHERE email = 'demo_seed_5@w2m-demo.local';

-- ===== 3) 미리 만들어 둔 Gemini 설명 (시연 중 Gemini 호출 실패에 대비한 저장본) =====
-- 스타벅스 공덕점
INSERT INTO place_descriptions (place_id, description, review_count, generated_at) VALUES ('1201355052', '평균 평점 4.2점을 기록한 스타벅스 공덕점은 넓은 자리와 많은 콘센트, 큰 테이블이 있어 모임이나 작업하기 편해요. 역과 가까워 길 찾기 쉽고 익숙한 맛을 즐길 수 있지만, 점심시간에는 줄이 길 수 있어요.', 5, NOW()) ON DUPLICATE KEY UPDATE description = VALUES(description), review_count = VALUES(review_count), generated_at = VALUES(generated_at);
-- 스트라다로스터스 공덕점
INSERT INTO place_descriptions (place_id, description, review_count, generated_at) VALUES ('1447732810', '진한 원두 향과 훌륭한 커피 맛을 즐길 수 있는 곳이에요. 넓고 천장이 높아 개방감이 좋으며 역에서 가까워 약속 장소로 편하게 방문하기 좋아요.', 5, NOW()) ON DUPLICATE KEY UPDATE description = VALUES(description), review_count = VALUES(review_count), generated_at = VALUES(generated_at);
-- 영앤도터스
INSERT INTO place_descriptions (place_id, description, review_count, generated_at) VALUES ('1419437580', '평균 평점 4.4점의 영앤도터스는 채광이 좋고 과하지 않은 인테리어로 편안한 분위기를 자랑합니다. 넓은 테이블과 잘 터지는 와이파이, 그리고 밝고 친절한 직원분들 덕분에 노트북 작업하기 좋은 곳입니다.', 5, NOW()) ON DUPLICATE KEY UPDATE description = VALUES(description), review_count = VALUES(review_count), generated_at = VALUES(generated_at);
-- 키하라
INSERT INTO place_descriptions (place_id, description, review_count, generated_at) VALUES ('1963225278', '동네 분위기가 느껴지는 편안한 키하라는 평균 평점 4.4점과 5개의 리뷰를 기록하고 있습니다. 아담한 자리와 잔잔한 음악 덕분에 혼자 책을 읽거나 소수 인원이 모여 대화하기 좋으며, 친절한 사장님 덕분에 따뜻한 시간을 보낼 수 있습니다.', 5, NOW()) ON DUPLICATE KEY UPDATE description = VALUES(description), review_count = VALUES(review_count), generated_at = VALUES(generated_at);
-- 포멜로빈 공덕점
INSERT INTO place_descriptions (place_id, description, review_count, generated_at) VALUES ('1246535644', '공덕역에서 가깝고 깔끔한 커피를 즐길 수 있는 포멜로빈 공덕점은 평균 평점 4.4점, 리뷰 5개를 기록하고 있습니다. 넓은 테이블과 넉넉한 자리 덕분에 노트북 작업이나 모임을 하기 좋으며, 친절한 바리스타가 원두 설명도 잘해줍니다.', 5, NOW()) ON DUPLICATE KEY UPDATE description = VALUES(description), review_count = VALUES(review_count), generated_at = VALUES(generated_at);
