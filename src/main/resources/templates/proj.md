# Project: W2M (Where2Meet) Backend - Progress Report

## 1. 현재 진행 상황 (Current Status)

### ✅ 이메일 인증 (SMTP + Redis)
- **EmailService**: Thymeleaf 템플릿(`MailForm.html`)을 사용한 인증 메일 발송 로직 구현 완료.
- **VerificationService**: Redis를 이용한 인증 코드 저장(TTL 10분) 및 검증 성공 시 "OK 플래그" 저장(TTL 30분) 로직 구현 완료.
- **MailController**: `/api/v1/send-code`, `/api/v1/verify-code` 엔드포인트 제공.

### ✅ 일반 회원가입 및 로그인 (Local Auth)
- **UserService**: BCrypt 비밀번호 암호화 및 가입/로그인 로직 구현.
- **UserController**: 
    - `/register`: 회원가입 시 Redis의 "OK 플래그"를 반드시 확인하도록 보안 강화 완료.
    - `/user/login`: JWT 발급 로직 포함.
    - `/logout`: Redis 블랙리스트를 통한 토큰 무효화 로직 포함.

### ✅ 통합 카카오 로그인 및 자동 가입 (OAuth2)
- **통합 로그인 정책**: 기존 일반(LOCAL) 계정과 소셜(KAKAO) 계정의 이메일이 동일할 경우, 별도의 차단 없이 통합 로그인을 허용하도록 개선.
- **자동 회원가입(Auto Signup)**: 신규 소셜 유저의 경우 추가 정보 입력 없이 카카오 정보를 바탕으로 DB에 즉시 저장되도록 로직 변경.
- **세션 정책 최적화**: OAuth2 인증 state 검증을 위해 `IF_REQUIRED` 세션 정책 적용.
- **프론트엔드 라우팅 연동**: 로그인 성공 후 `/kakao-callback` 경로를 통한 토큰 처리 및 페이지 이동 로직 연동 완료.

### ✅ 모임(Meeting) 및 참여자(Participant) 관리
- **MeetingController/Service**: 모임 생성, 상세 조회, 내 모임 목록 조회 API 구현.
- **사용자 인증 연동**: `Authentication` 객체를 통해 로그인한 사용자의 ID를 추출하여 모임 생성 및 조회 시 소유권 확인 로직 적용.

### ✅ 가능 시간(Availability) 관리 시스템
- **Entity 설계**: 
    - `Participant`: 회원/비회원을 아우르는 모임 참여자 관리.
    - `Availability`: 참여자별 `LocalDateTime` 기반의 가능 시간 범위 저장.
- **일괄 처리 로직 (Bulk Save)**: 대량의 시간 데이터를 효율적으로 처리하기 위해 "기존 데이터 삭제 후 일괄 저장(Delete-then-Insert)" 방식 채택.
- **DTO 최적화**: `@JsonFormat`을 사용하여 ISO 8601 형식의 날짜 데이터 정합성 보장 및 유효성 검증 추가.
- **AvailabilityController**: 가능 시간 저장(POST), 수정(PUT), 조회(GET) API 구현 완료.

---

## 2. 인증 시스템 작동 방식 상세 가이드 (Deep Dive)

### 🛡️ 공통 원칙
1. **이메일 기반 통합**: 시스템 내 모든 이메일은 유일해야 하며, 동일 이메일인 경우 가입 경로에 상관없이 로그인을 허용함.
2. **신규 유저 즉시 가입**: 카카오 로그인을 통해 처음 접속하는 유저는 즉시 DB에 계정을 생성하여 사용자 편의성을 극대화함.

---

### 📧 [FLOW 1] 로컬 회원가입 & 로그인
1. **인증 단계**: 사용자가 이메일로 6자리 코드를 받고 검증을 마침. Redis에 `email:verify:ok:[email]` 키가 생성됨.
2. **가입 단계**: `/api/v1/register` 호출 시 서버는 위 Redis 키가 있는지 확인. 없으면 즉시 차단.
3. **완료**: 비밀번호를 BCrypt로 암호화하여 `Provider.LOCAL` 타입으로 DB 저장.
4. **로그인**: 이메일/비밀번호 대조 후 성공 시 우리 서버 전용 JWT 발급.

---

### 🟡 [FLOW 2] 카카오 소셜 로그인 & 통합 가입
1. **인증 시작**: 프론트엔드에서 카카오 로그인 요청.
2. **유저 판별 (`CustomOAuth2UserService`)**:
    - **기존 유저 (로컬/소셜 공통)**: 이메일이 존재하면 해당 계정으로 즉시 로그인 처리.
    - **신규 유저**: 존재하지 않는 이메일일 경우, 카카오 정보를 바탕으로 **즉시 DB 저장(`Provider.KAKAO`)** 후 로그인 처리.
3. **성공 처리 (`OAuth2SuccessHandler`)**:
    - 로그인 성공 시 JWT를 생성하여 프론트엔드 콜백 경로(`http://localhost:5173/kakao-callback?token=...`)로 리다이렉트.
4. **최종 처리**: 프론트엔드에서 토큰을 저장하고 메인 화면으로 이동.

---

## 3. 향후 로드맵 (Roadmap)

### Step 1: 보안 및 검증 강화
- [ ] `RegisterRequestDto` 및 `SocialRegisterRequestDto`에 `@Valid` 어노테이션을 통한 입력값 복잡도 검사 추가.
- [ ] `JwtTokenFilter`에서 로그아웃 블랙리스트 체크 로직 재검증.

### Step 2: 시스템 고도화
- [ ] **확정 로직 구현**: 참여자들의 공통 가능 시간을 계산하여 최적의 약속 시간을 추천하고 확정하는 알고리즘 개발.
- [ ] **Refresh Token 도입**: Access Token 만료에 대비한 Redis 기반 리프레시 토큰 로직.
- [ ] **Global Exception Handler**: 인증 에러 발생 시 사용자에게 친절한 JSON 응답을 내려주도록 `@RestControllerAdvice` 구현.

---

## 4. 작업 로그 (Task Logs)
- **2026-05-01**: 프로젝트 초기 분석 및 `proj.md` 작성.
- **2026-05-01**: `CustomUserDetails` 수정 및 OAuth2 기초 로직 구현.
- **2026-05-21**: **카카오 로그인 통합 및 정상화 업데이트**
    - **통합 로그인**: LOCAL/KAKAO 계정 구분 없이 동일 이메일이면 로그인 허용 로직 적용.
    - **자동 회원가입**: 신규 소셜 유저 즉시 DB 저장 로직 구현.
    - **보안 설정 수정**: `SecurityConfig` 세션 정책 변경(`STATELESS` -> `IF_REQUIRED`) 및 `/oauth2/**` 경로 허용.
    - **환경 설정 최적화**: `application.yml` 인증 방식 충돌 해결 및 상세 로그(`DEBUG`) 추가.
    - **프론트엔드 연동**: `AppRouter.jsx`에 `/kakao-callback` 라우팅 추가하여 빈 화면 문제 해결.
- **2026-05-28**: **약속 시간 조율 시스템 고도화**
    - **Entity & Repository**: `Availability`, `Participant` 엔티티 및 저장소 구현.
    - **API 고도화**: 사용자 인증 정보를 활용한 모임 생성 및 조회 로직 완성.
    - **대량 데이터 처리**: 항공권 예약 스타일의 대량 시간 범위 데이터를 처리하기 위한 벌크 저장 서비스 구현.
    - **DTO 최적화**: 프론트엔드와의 날짜 포맷팅(`ISO 8601`) 통일 및 유효성 검사 강화.
