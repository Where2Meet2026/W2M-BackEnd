package com.w2m.backend.auth.jwt;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j // ✅ 2. 클래스에 Slf4j 어노테이션 추가
public class JwtTokenFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final String secretKey;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // ✅ 요청이 들어온 URI 로깅
        log.info("Request received for URI: {}", request.getRequestURI());

        // Header의 Authorization 값이 비어있으면 => Jwt Token을 전송하지 않음 => 로그인 하지 않음
        if (authorizationHeader == null) {
            log.warn("Authorization Header is missing. Passing request to the next filter.");
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization Header does not start with 'Bearer '. Header: {}", authorizationHeader);
            filterChain.doFilter(request, response);
            return;
        }

        // 전송받은 값에서 'Bearer ' 뒷부분(Jwt Token) 추출
        final String token = authorizationHeader.split(" ")[1];

        // 전송받은 Jwt Token이 만료되었으면 => 다음 필터 진행(인증 X)
        if (JwtTokenUtil.isExpired(token, secretKey)) {
            log.warn("Token is expired. Token: {}", token);
            filterChain.doFilter(request, response);
            return;
        }

        // Redis에 해당 토큰이 블랙리스트로 등록되어 있는지 확인
        String isLogout = (String) redisTemplate.opsForValue().get(token);
        if (isLogout != null && isLogout.equals("logout")) {
            log.warn("Access attempt with a logged-out token. Token: {}", token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }


        // Jwt Token에서 아이디 추출
        String id = JwtTokenUtil.getLoginId(token, secretKey);

        // 추출한 id(pk)로 User 찾기
        User loginUser = userService.getLoginUserById(Long.parseLong(id));
        log.info("JwtTokenFilter - loginUser from DB: {}", loginUser);

        CustomUserDetails principal = new CustomUserDetails(loginUser);
        log.info("JwtTokenFilter - CustomUserDetails Principal: {}", principal);



        // loginUser 정보로 UsernamePasswordAuthenticationToken 발급
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        log.info("JwtTokenFilter - SecurityContext Authentication set: {}", SecurityContextHolder.getContext().getAuthentication());

        // 권한 부여
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.info("Authentication successful. Setting security context for user: {}", id);

        filterChain.doFilter(request, response);
    }
}
