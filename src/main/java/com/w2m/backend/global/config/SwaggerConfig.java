package com.w2m.backend.global.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    //http://localhost:8080/swagger-ui/index.html
    private final String AUTH_TOKEN_HEADER = "Authorization";

    private Info apiInfo() {
        return new Info()
                .title("Swagger API")
                .description("""
                        Swagger API 테스트

                        <details>
                        <summary>📋 Swagger 테스트 흐름 (백엔드 단독 테스트 순서) — 펼쳐서 보기</summary>

                        지금까지 완성된 기능 기준으로, 실제 서비스가 진행되는 순서 그대로 따라가면 됩니다. 뒤 단계가 앞 단계 결과값(토큰, meetingId 등)을 필요로 하는 구조예요.

                        1. **로그인** `POST /api/auth/user/login` → 응답의 `token` 복사
                        2. **Authorize** 버튼에 `Bearer {token}` 넣기 (한 번만 하면 계속 유지됨)
                        3. **모임 생성** `POST /api/meetings` → 응답에서 `meetingId`, `inviteCode` 확보 (자동으로 방장(HOST) 부여)
                        4. **(선택) 다른 계정으로 참여**: 다른 계정 로그인 → `POST /api/meetings/join`에 3번의 `inviteCode` 입력
                            - 혼자 테스트할 거면 생략 가능
                        5. **참여자 목록 조회** `GET /api/meetings/{meetingId}/participants` → `participantId` 확보
                        6. **가능 시간 입력** `POST /api/participants/{participantId}/availabilities`
                        7. **추천 상태 조회** `GET /api/meetings/{meetingId}/recommendations/status` → 전원 입력 시 `true`
                        8. **추천 목록 조회** `GET /api/meetings/{meetingId}/recommendations`
                        9. **시간 확정** `PATCH /api/meetings/{meetingId}/confirmed-time`

                        </details>
                        """)
                .version("1.0.0");
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(AUTH_TOKEN_HEADER))
                .components(new Components()
                        .addSecuritySchemes(AUTH_TOKEN_HEADER, new SecurityScheme()
                                .name(AUTH_TOKEN_HEADER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                );
    }
}
