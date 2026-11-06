package com.vibeport.ai.util;

import com.vibeport.ai.vo.ConcertInfoVo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI가 돌려준 공연 시간/예매 일시를 DB 저장 형식으로 통일합니다.
 * <ul>
 *   <li>concertTime: {@code HHmm}, 같은 날 여러 회차는 {@code HHmm/HHmm}</li>
 *   <li>tctOpenAt: {@code yyyy-MM-dd HH:mm} (시간 미상이면 {@code yyyy-MM-dd})</li>
 * </ul>
 * 형식을 해석할 수 없는 값(예: "미정", 한글 서술)은 원본을 그대로 둡니다.
 */
public final class ConcertFieldNormalizer {

    // 2026-10-27T14:00, 2026-10-27 14:00:00, 2026.10.27 14:00, 2026/10/27T14:00+09:00 등
    private static final Pattern DATE_TIME = Pattern.compile(
            "^(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})(?:\\s*[T ]\\s*(\\d{1,2}):(\\d{2})(?::\\d{2}(?:\\.\\d+)?)?)?"
                    + "\\s*(?:Z|[+-]\\d{2}:?\\d{2}|KST)?$",
            Pattern.CASE_INSENSITIVE);

    // 19:00, 1900, 9:30
    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):?(\\d{2})$");

    private static final Pattern TIME_SEPARATOR = Pattern.compile("\\s*[/,|~]\\s*");

    private ConcertFieldNormalizer() {
    }

    public static void normalize(ConcertInfoVo concert) {
        if (concert == null) {
            return;
        }
        concert.setConcertTime(normalizeConcertTime(concert.getConcertTime()));
        concert.setTctOpenAt(normalizeTctOpenAt(concert.getTctOpenAt()));
    }

    public static String normalizeTctOpenAt(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        Matcher m = DATE_TIME.matcher(trimmed);
        if (!m.matches()) {
            return trimmed;
        }

        int month = Integer.parseInt(m.group(2));
        int day = Integer.parseInt(m.group(3));
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            return trimmed;
        }
        String date = String.format("%s-%02d-%02d", m.group(1), month, day);
        if (m.group(4) == null) {
            return date;
        }

        int hour = Integer.parseInt(m.group(4));
        int minute = Integer.parseInt(m.group(5));
        if (hour > 23 || minute > 59) {
            return trimmed;
        }
        return String.format("%s %02d:%02d", date, hour, minute);
    }

    public static String normalizeConcertTime(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        String[] parts = TIME_SEPARATOR.split(trimmed);
        List<String> times = new ArrayList<>(parts.length);
        for (String part : parts) {
            Matcher m = TIME.matcher(part.trim());
            if (!m.matches()) {
                return trimmed;
            }
            int hour = Integer.parseInt(m.group(1));
            int minute = Integer.parseInt(m.group(2));
            if (hour > 23 || minute > 59) {
                return trimmed;
            }
            String time = String.format("%02d%02d", hour, minute);
            if (!times.contains(time)) {
                times.add(time);
            }
        }
        return String.join("/", times);
    }
}
