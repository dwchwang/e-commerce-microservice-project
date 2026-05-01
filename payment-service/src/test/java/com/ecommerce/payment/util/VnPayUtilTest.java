package com.ecommerce.payment.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VnPayUtilTest {

    @Test
    void formatDateTreatsLocalDateTimeAsVietnamTime() {
        assertThat(VnPayUtil.formatDate(LocalDateTime.of(2026, 5, 1, 12, 30, 45)))
                .isEqualTo("20260501123045");
    }
}
