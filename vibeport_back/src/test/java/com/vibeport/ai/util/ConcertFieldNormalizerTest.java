package com.vibeport.ai.util;

import com.vibeport.ai.vo.ConcertInfoVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConcertFieldNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2026-10-27T14:00          | 2026-10-27 14:00",
            "2026-10-27 14:00          | 2026-10-27 14:00",
            "2026-10-27T14:00:00       | 2026-10-27 14:00",
            "2026-10-27T14:00:00.000Z  | 2026-10-27 14:00",
            "2026-10-27T14:00+09:00    | 2026-10-27 14:00",
            "2026-10-27 14:00 KST      | 2026-10-27 14:00",
            "2026.10.27 14:00          | 2026-10-27 14:00",
            "2026/1/7 9:05             | 2026-01-07 09:05",
            "2026-10-27                | 2026-10-27",
            "미정                       | 미정",
            "11월 12일 오후 8시          | 11월 12일 오후 8시",
            "2026-13-01 10:00          | 2026-13-01 10:00",
    })
    void normalizeTctOpenAt(String input, String expected) {
        assertThat(ConcertFieldNormalizer.normalizeTctOpenAt(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1900            | 1900",
            "19:00           | 1900",
            "9:30            | 0930",
            "1500/1900       | 1500/1900",
            "15:00 / 19:00   | 1500/1900",
            "15:00, 19:00    | 1500/1900",
            "1900/1900       | 1900",
            "미정             | 미정",
            "0000            | 0000",
            "오후 7시         | 오후 7시",
            "2500            | 2500",
    })
    void normalizeConcertTime(String input, String expected) {
        assertThat(ConcertFieldNormalizer.normalizeConcertTime(input)).isEqualTo(expected);
    }

    @Test
    void normalizeKeepsNullAndBlank() {
        assertThat(ConcertFieldNormalizer.normalizeTctOpenAt(null)).isNull();
        assertThat(ConcertFieldNormalizer.normalizeConcertTime("")).isEmpty();

        ConcertInfoVo vo = new ConcertInfoVo();
        vo.setConcertTime("15:00 / 19:00");
        vo.setTctOpenAt("2026-08-10T13:00");
        ConcertFieldNormalizer.normalize(vo);
        assertThat(vo.getConcertTime()).isEqualTo("1500/1900");
        assertThat(vo.getTctOpenAt()).isEqualTo("2026-08-10 13:00");
    }
}
