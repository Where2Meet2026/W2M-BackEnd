package com.w2m.backend.meeting.dto;

import com.w2m.backend.meeting.dto.request.CreateMeetingRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// 모임 이름은 비어 있으면 안 되고 100자, 설명은 500자까지 (meetings 컬럼 길이). Spring 없이 Bean Validation만 확인.
class CreateMeetingRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static CreateMeetingRequest request(String title, String description) {
        CreateMeetingRequest request = new CreateMeetingRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "description", description);
        return request;
    }

    private static Set<String> messages(CreateMeetingRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void 정상_입력은_통과한다() {
        assertThat(validator.validate(request("마포구 주민들", "저녁 모임"))).isEmpty();
        assertThat(validator.validate(request("마포구 주민들", null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void 모임_이름이_비어_있으면_거부한다(String title) {
        assertThat(messages(request(title, "설명"))).containsExactly("모임 이름을 입력해주세요.");
    }

    @Test
    void 모임_이름이_없으면_거부한다() {
        assertThat(messages(request(null, "설명"))).containsExactly("모임 이름을 입력해주세요.");
    }

    @Test
    void 모임_이름은_100자까지_허용한다() {
        assertThat(validator.validate(request("가".repeat(100), null))).isEmpty();
        assertThat(messages(request("가".repeat(101), null))).containsExactly("모임 이름은 100자 이하로 입력해주세요.");
    }

    @Test
    void 설명은_500자까지_허용한다() {
        assertThat(validator.validate(request("모임", "가".repeat(500)))).isEmpty();
        assertThat(messages(request("모임", "가".repeat(501)))).containsExactly("모임 설명은 500자 이하로 입력해주세요.");
    }
}
