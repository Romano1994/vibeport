package com.vibeport.ai.service;

import com.vibeport.VibePortApplication;
import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.vo.ConcertInfoVo;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {VibePortApplication.class, ConcertMissingFieldManualTests.MapperTestConfig.class})
class ConcertMissingFieldManualTests {

    private static final String UNKNOWN = new String(new char[] {(char) 48120, (char) 51221});

    @TestConfiguration
    @MapperScan("com.vibeport.ai.mapper")
    static class MapperTestConfig {
    }

    private final AiMapper aiMapper;
    private final ConcertPipelineService concertPipelineService;

    @Autowired
    ConcertMissingFieldManualTests(AiMapper aiMapper, ConcertPipelineService concertPipelineService) {
        this.aiMapper = aiMapper;
        this.concertPipelineService = concertPipelineService;
    }

    @Test
    void enrichMissingConcertFieldsWithConcertNoFromDatabase() {
        int concertNo = 59;
        ConcertInfoVo concert = aiMapper.selectConcertInfoByNo(concertNo);

        assertNotNull(concert, "No concert found for CONCERT_NO=" + concertNo);
        assertTrue(hasMissingField(concert), "Selected concert has no missing target fields: CONCERT_NO=" + concertNo);

        ConcertInfoVo before = copyOf(concert);

        List<ConcertInfoVo> result = concertPipelineService.enrichMissingConcertFields(List.of(concert));

        assertEquals(1, result.size());
        assertEquals(before.getConcertNo(), result.get(0).getConcertNo());
        assertNormalizedMissingFields(result.get(0));

        System.out.printf(
                "CONCERT_NO=%d before=[time=%s, venue=%s, tctSite=%s, tctOpenAt=%s] after=[time=%s, venue=%s, tctSite=%s, tctOpenAt=%s]%n",
                concertNo,
                before.getConcertTime(), before.getVenue(), before.getTctSite(), before.getTctOpenAt(),
                result.get(0).getConcertTime(), result.get(0).getVenue(), result.get(0).getTctSite(), result.get(0).getTctOpenAt()
        );
    }

    private boolean hasMissingField(ConcertInfoVo concert) {
        return isMissingConcertTime(concert.getConcertTime())
                || isMissingText(concert.getVenue())
                || isMissingText(concert.getTctSite())
                || isMissingText(concert.getTctOpenAt());
    }

    private void assertNormalizedMissingFields(ConcertInfoVo concert) {
        assertNotNull(concert.getConcertTime());
        assertNotNull(concert.getVenue());
        assertNotNull(concert.getTctSite());
        assertNotNull(concert.getTctOpenAt());
    }

    private boolean isMissingConcertTime(String value) {
        return isMissingText(value) || "0000".equals(value.trim());
    }

    private boolean isMissingText(String value) {
        return value == null || value.isBlank() || UNKNOWN.equals(value.trim());
    }

    private ConcertInfoVo copyOf(ConcertInfoVo source) {
        ConcertInfoVo copy = new ConcertInfoVo();
        copy.setConcertNo(source.getConcertNo());
        copy.setConcertTime(source.getConcertTime());
        copy.setVenue(source.getVenue());
        copy.setTctSite(source.getTctSite());
        copy.setTctOpenAt(source.getTctOpenAt());
        return copy;
    }
}
