package com.w2m.backend.auth.controller;


import com.w2m.backend.auth.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MailController {

    private final VerificationService verificationService;


    //인증코드 전송
    @PostMapping("/send-code")
    public ResponseEntity<Object> sendCode(@RequestParam String email) {
        if(verificationService.checkEmailDuplicate(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("이미 가입된 이메일입니다.");
        }
        else{
            verificationService.sendCode(email);
            return ResponseEntity.ok().build();
        }
    }

    //코드 검증
    @PostMapping("/verify-code")
    public ResponseEntity<Boolean> verifyCode(@RequestParam String email, @RequestParam String code) {
        boolean ok = verificationService.verifyCode(email, code);
        return ResponseEntity.ok(ok);
    }

    //인증 상태 확인 (프론트에서 폼 제출 전 체크용)
    @GetMapping("/status")
    public ResponseEntity<Boolean> status(@RequestParam String email) {
        return ResponseEntity.ok(verificationService.isVerified(email));
    }
}
