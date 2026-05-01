import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.auth.jwt.JwtTokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${SecretKey}")
    private String secretKey;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        // 1. 유저 정보 추출
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // [신규 유저인 경우] - 아직 DB에 저장되지 않음 (id가 null)
        if (user.getId() == null) {
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/social-signup")
                    .queryParam("email", user.getEmail())
                    .queryParam("name", user.getName())
                    .queryParam("providerId", user.getProviderId())
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // [기존 유저인 경우] - JWT 토큰 생성 및 리다이렉트
        long expireTimeMs = 1000 * 60 * 60;
        String jwtToken = JwtTokenUtil.createToken(user.getId(), secretKey, expireTimeMs);

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token", jwtToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
