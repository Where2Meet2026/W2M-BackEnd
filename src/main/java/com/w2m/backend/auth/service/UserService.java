package com.w2m.backend.auth.service;

import com.w2m.backend.auth.dto.LoginRequestDto;
import com.w2m.backend.auth.dto.RegisterRequestDto;
import com.w2m.backend.auth.dto.SocialRegisterRequestDto;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {


    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    //id 중복체크
    public boolean checkLoginIdDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }


    //회원가입
    public void join(RegisterRequestDto registerRequestDto) {
        userRepository.save(registerRequestDto.toEntity(bCryptPasswordEncoder.encode(registerRequestDto.getPassword())));
    }

    //소셜 회원가입
    public void socialJoin(SocialRegisterRequestDto socialRegisterRequestDto) {
        userRepository.save(socialRegisterRequestDto.toEntity());
    }

    //로그인
    public User userLogin(LoginRequestDto req) {
        Optional<User> optionalUser = userRepository.findByEmail(req.getEmail());

        // loginId와 일치하는 User가 없으면 null return
        if(optionalUser.isEmpty()) {
            return null;
        }

        User user = optionalUser.get();

        // 찾아온 User의 password와 입력된 password가 다르면 null return
        if (!bCryptPasswordEncoder.matches(req.getPassword(), user.getPassword())) {
            return null;
        }

        return user;
    }



    //userId(Long)를 입력받아 User을 return 해주는 기능
    public User getLoginUserById(Long userId) {
        if(userId == null) return null;

        Optional<User> optionalUser = userRepository.findById(userId);
        if(optionalUser.isEmpty()) return null;

        return optionalUser.get();
    }
}
