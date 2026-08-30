# W2M-Backend

Where2Meet(W2M) 서비스의 백엔드입니다.

---

## 기술 스택

| 구분 | 내용 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.3 |
| Build Tool | Gradle |
| ORM | Spring Data JPA (Hibernate) |
| DB | MariaDB |
| Cache / Session | Redis |
| 인증 | Spring Security, JWT(jjwt), OAuth2 Client(카카오 로그인) |
| 메일 발송 | Spring Mail (SMTP) + Thymeleaf |
| API 문서 | springdoc-openapi (Swagger UI) |

---

## 요구사항

프로젝트를 실행하기 전에 아래가 준비되어 있어야 합니다.

- JDK 17 이상
- 실행 중인 MariaDB
- 실행 중인 Redis

설치 여부는 아래 명령어로 확인할 수 있습니다.

```bash
java -version
```

---

## 실행 방법

### 1. 프로젝트 클론

```bash
git clone <repository-url>
cd W2M-BackEnd
```

### 2. 로컬 설정 파일 작성

DB 접속 정보, JWT secret, 카카오 OAuth client-secret, 메일 계정 등 민감한 값은
`src/main/resources/application-local.yml`에 작성합니다. 이 파일은 `.gitignore`에
등록되어 있어 Git에 올라가지 않으니, 팀원에게 직접 값을 공유받아 로컬에만
만들어두면 됩니다. (`application.yml`에는 민감하지 않은 공통 설정만 있습니다.)

### 3. 개발 서버 실행

```bash
./gradlew bootRun
```

기본적으로 `http://localhost:8080`에서 실행됩니다.

### 4. API 문서 확인

서버 실행 후 아래 주소에서 전체 API 목록과 요청/응답 형식을 확인할 수 있습니다.

```txt
http://localhost:8080/swagger-ui/index.html
```

---

## 주요 명령어

| 명령어 | 설명 |
|---|---|
| `./gradlew bootRun` | 개발 서버를 실행합니다. |
| `./gradlew compileJava` | 코드가 정상적으로 컴파일되는지만 빠르게 확인합니다. |
| `./gradlew test` | 테스트를 실행합니다. |
| `./gradlew build` | 배포용 jar 파일을 생성합니다. |

---

## 설정 파일 구조

| 파일 | 설명 |
|---|---|
| `application.yml` | 민감하지 않은 공통 설정 (mail 서버, 카카오 OAuth 엔드포인트 등). Git에 커밋합니다. |
| `application-local.yml` | DB 접속정보, JWT secret, OAuth client-secret 등 민감한 값. Git에 커밋하지 않습니다. |
| `build.gradle` | 프로젝트 정보와 의존 라이브러리 목록을 관리합니다. |

---

## 패키지 구조

코드는 레이어(controller/service/...)가 아니라 **도메인(기능) 단위**로 먼저 나뉘고,
그 안에서 다시 레이어별로 나뉩니다.

```txt
com.w2m.backend/
├─ global/            도메인에 속하지 않는 공통 코드 (예외 처리, Swagger 설정)
│
├─ auth/              회원가입 · 로그인 · 카카오 소셜로그인 · JWT 발급/검증 · 이메일 인증
├─ meeting/           모임 생성/조회/삭제, 초대 코드 발급, 약속 시간 확정
├─ participant/       모임 참여자 정보 (방장/참여자 역할), 참여·탈퇴·목록 조회
├─ availability/      참여자별 "가능한 시간대" 저장/조회
├─ recommendation/    참여자들의 가능 시간을 모아 공통 시간대 계산 및 추천
│
├─ location/          (2학기) 참여자별 출발 위치 좌표 저장 — 중간지점 계산의 재료
├─ candidate/         (2학기) 중간지점 주변 실제 장소 후보 산출 (빠른/균형/최적)
├─ vote/              (2학기) 장소 후보에 대한 좋아요/싫어요 투표 집계
├─ review/            (2학기) 확정된 장소에 대한 참여자 후기 — candidate의 AI 추천 설명 생성에 재료로 쓰임
└─ notification/      (2학기) Web Push 구독 정보 저장 및 확정 알림 발송
```

각 도메인 폴더 내부는 아래처럼 항상 같은 구조입니다.

```txt
{domain}/
├─ controller/   HTTP 요청을 받는 진입점 (@RestController)
├─ dto/
│  ├─ request/    클라이언트가 보내는 요청 바디 형태
│  └─ response/   클라이언트에게 돌려줄 응답 바디 형태
├─ service/      실제 비즈니스 로직
├─ repository/   DB 쿼리 (Spring Data JPA)
└─ entity/       DB 테이블과 매핑되는 클래스
```

요청 하나가 처리되는 흐름은 항상 `controller → service → repository → entity`
순서로 한 방향으로 흐릅니다. 예를 들어 "약속 시간 확정" 요청은
`meeting/controller/MeetingController` → `meeting/service/MeetingService`
→ `meeting/repository/MeetingRepository` → `meeting/entity/Meeting` 순으로 처리됩니다.

새 기능을 추가할 때, 기존 도메인에 속하면 그 폴더 안에 파일을 추가하고,
새로운 개념이면 최상위에 도메인 폴더를 새로 만들어 위와 같은 5개 하위 폴더
구조를 그대로 따릅니다.

---

## 브랜치 전략

| 브랜치 | 용도 |
|---|---|
| `main` | 배포 가능한 안정 버전 |
| `dev` | 개발 통합 브랜치 |
| `feature/*` | 기능 개발 브랜치 |
| `fix/*` | 버그 수정 브랜치 |

작업은 `dev` 브랜치에서 새 브랜치를 만들어 진행합니다.

```bash
git checkout dev
git checkout -b feature/location
```

작업 완료 후 GitHub에서 Pull Request를 생성하고, 리뷰 후 `dev` 브랜치에 병합합니다.

---

## 커밋 메시지 예시

| 타입 | 설명 | 예시 |
|---|---|---|
| `init` | 프로젝트 초기 설정 | `init: project setup` |
| `feat` | 새로운 기능 추가 | `feat: add location save api` |
| `fix` | 버그 수정 | `fix: handle expired jwt token` |
| `docs` | 문서 수정 | `docs: update README` |
| `refactor` | 코드 구조 개선 | `refactor: split auth dto by request/response` |

---

## Git에 올리지 않는 파일

```txt
build/
.gradle/
.idea/
src/main/resources/application-local.yml
```

- `build/`, `.gradle/`: 빌드 결과물 및 캐시. `./gradlew build`로 다시 생성됩니다.
- `application-local.yml`: DB/JWT/OAuth 등 민감한 로컬 설정값입니다.
