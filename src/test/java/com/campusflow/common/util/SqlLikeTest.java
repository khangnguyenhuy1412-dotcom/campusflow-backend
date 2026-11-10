package com.campusflow.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlLikeTest {

    @Test
    void likeWildcardsAreEscaped() {
        assertThat(SqlLike.escape("100%_off\\")).isEqualTo("100\\%\\_off\\\\");
        assertThat(SqlLike.escape("giao trinh")).isEqualTo("giao trinh");
    }

    @Test
    void containsPatternIsTrimmedAndLowercased() {
        assertThat(SqlLike.contains("  Giải TÍCH ")).isEqualTo("%giải tích%");
    }
}
