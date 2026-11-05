package com.w2m.backend.review.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.w2m.backend.review.dto.request.CreateReviewRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

// 별점은 0.5 ~ 5.0, 0.5 단위만 허용한다. (Spring 없이 Bean Validation만 확인)
class CreateReviewRequestValidationTest {

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

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 1.5, 2.5, 3.0, 4.5, 5.0})
    void 반점_단위_별점은_통과한다(double rating) {
        assertThat(validator.validate(new CreateReviewRequest(rating, "좋아요"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.4, 0.75, 1.2, 2.7, 4.9, 5.5, 6.0, -1.0})
    void 범위를_벗어나거나_0점5_단위가_아니면_거부한다(double rating) {
        assertThat(validator.validate(new CreateReviewRequest(rating, "좋아요"))).isNotEmpty();
    }

    @Test
    void 별점이_없으면_거부한다() {
        assertThat(validator.validate(new CreateReviewRequest(null, "좋아요"))).isNotEmpty();
    }

    @Test
    void 정수_JSON도_그대로_받는다() throws Exception {
        CreateReviewRequest request = new ObjectMapper().readValue("{\"rating\":4,\"comment\":\"ok\"}", CreateReviewRequest.class);

        assertThat(request.getRating()).isEqualTo(4.0);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 소수_JSON을_받는다() throws Exception {
        CreateReviewRequest request = new ObjectMapper().readValue("{\"rating\":2.5}", CreateReviewRequest.class);

        assertThat(request.getRating()).isEqualTo(2.5);
        assertThat(validator.validate(request)).isEmpty();
    }
}
