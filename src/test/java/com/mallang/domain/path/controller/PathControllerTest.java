package com.mallang.domain.path.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.path.enums.PathType;
import com.mallang.domain.path.service.PathActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PathControllerTest {

    private final PathController controller = new PathController(new PathActivityService());

    @Test
    void SWITCH이면_활동_세_개를_돌려준다() {
        assertThat(controller.actions(PathType.SWITCH).data().activities()).hasSize(3);
    }

    @Test
    void SWITCH가_아니면_400이다() {
        assertThatThrownBy(() -> controller.actions(PathType.ACTION))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}