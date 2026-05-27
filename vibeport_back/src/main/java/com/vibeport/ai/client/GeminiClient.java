package com.vibeport.ai.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.ai.vo.ConcertInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.springframework.core.io.ClassPathResource;

import net.coobird.thumbnailator.Thumbnails;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class GeminiClient {

    private static final String GEMINI_MISSING_FIELD_MODEL = "gemini-2.5-pro";
    private static final int MISSING_FIELD_TIMEOUT_MS = 180_000;
    private static final int MISSING_FIELD_MAX_OUTPUT_TOKENS = 4096;

    // 아티스트 이미지 저장 시 리사이징 기준 (미리보기 max-width 500px의 레티나 2배)
    private static final int MAX_IMAGE_DIMENSION = 1000;
    private static final double JPEG_QUALITY = 0.8d;

    private final Client client;
    private final Tool googleSearchTool;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${serpapi.api-key}")
    private String serpApiKey;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @Value("${app.image-dir:./data/concert_images/}")
    private String imageDir;

    public GeminiClient(@Value("${spring.ai.gemini.api-key}") String geminiApiKey) {
        this.client = Client.builder()
                .apiKey(geminiApiKey)
                .build();

        this.googleSearchTool = Tool.builder()
                .googleSearch(GoogleSearch.builder().build())
                .build();
    }

    public List<ConcertInfoVo> getConcertInfos(int year, int month) throws Exception{
        List<ConcertInfoVo> resultList;

        String systemPrompt = loadPrompt("prompts/concert-info-system.txt");

        Content systemInstruction = Content.builder()
                .parts(List.of(Part.builder().text(systemPrompt).build()))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(List.of(googleSearchTool))
                .systemInstruction(systemInstruction)
                .temperature(0.0f)
                .build();

        String userPrompt = "### 역할 : USER\n" +
                year + "년 " + month + "월에 내한이 확정된 가수와 콘서트 정보를 알려줘.";

        log.info(year + "=============" + month);
        GenerateContentResponse response = client.models.generateContent(
                "gemini-3-pro-preview",
                Content.builder().parts(List.of(Part.builder().text(userPrompt).build())).build(),
                config
        );

        log.info("response==========" + response.text());

        resultList = this.concertInfoReprocess(response.text());
        return resultList;
    }

    public List<ConcertInfoVo> getDailyConcertInfos() throws Exception {
        List<ConcertInfoVo> resultList;

        String systemPrompt = loadPrompt("prompts/daily-concert-info-system.txt");

        Content systemInstruction = Content.builder()
                .parts(List.of(Part.builder().text(systemPrompt).build()))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(List.of(googleSearchTool))
                .systemInstruction(systemInstruction)
                .temperature(0.0f)
                .build();

        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));

        String userPrompt = "### 역할 : USER\n" +
                dateStr + "에 새로 발표된 내한 콘서트 정보를 알려줘.";

        log.info("daily concert search date: {}", dateStr);
        GenerateContentResponse response = client.models.generateContent(
                "gemini-3-pro-preview",
                Content.builder().parts(List.of(Part.builder().text(userPrompt).build())).build(),
                config
        );

        log.info("daily response==========" + response.text());

        resultList = this.concertInfoReprocess(response.text());
        return resultList;
    }

    public ConcertInfoVo fetchMissingConcertFields(ConcertInfoVo concert, Set<String> missingFields) {
        if (concert == null || missingFields == null || missingFields.isEmpty()) {
            return null;
        }

        String systemPrompt = loadPrompt("prompts/gemini-fill-missing-concert-fields-system.txt");
        Content systemInstruction = Content.builder()
                .parts(List.of(Part.builder().text(systemPrompt).build()))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(List.of(googleSearchTool))
                .systemInstruction(systemInstruction)
                .temperature(0.0f)
                .maxOutputTokens(MISSING_FIELD_MAX_OUTPUT_TOKENS)
                .httpOptions(HttpOptions.builder().timeout(MISSING_FIELD_TIMEOUT_MS))
                .build();

        String userPrompt = buildMissingFieldsPrompt(concert, missingFields);

        try {
            log.info("[GeminiClient] fetchMissingConcertFields request - artistNmKor={}, date={}-{}-{}, fields={}",
                    concert.getArtistNmKor(),
                    concert.getConcertYear(), concert.getConcertMonth(), concert.getConcertDate(),
                    missingFields);
            GenerateContentResponse response = client.models.generateContent(
                    GEMINI_MISSING_FIELD_MODEL,
                    Content.builder().parts(List.of(Part.builder().text(userPrompt).build())).build(),
                    config
            );
            log.info("[GeminiClient] missing-field response: {}", response.text());
            return parseJsonObjectResponse(response.text());
        } catch (Exception e) {
            log.error("[GeminiClient] missing-field request failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildMissingFieldsPrompt(ConcertInfoVo concert, Set<String> missingFields) {
        return """
                Fill only the requested missing fields for this concert.

                Missing fields: %s

                Existing concert:
                - artistNmKor: %s
                - artistNmFor: %s
                - concertYear: %d
                - concertMonth: %d
                - concertDate: %d
                - concertTime: %s
                - venue: %s
                - tctSite: %s
                - tctOpenAt: %s
                - genre: %s
                """.formatted(
                missingFields,
                valueOrUnknown(concert.getArtistNmKor()),
                valueOrUnknown(concert.getArtistNmFor()),
                concert.getConcertYear(),
                concert.getConcertMonth(),
                concert.getConcertDate(),
                valueOrUnknown(concert.getConcertTime()),
                valueOrUnknown(concert.getVenue()),
                valueOrUnknown(concert.getTctSite()),
                valueOrUnknown(concert.getTctOpenAt()),
                valueOrUnknown(concert.getGenre())
        );
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank()
                ? new String(new char[] {(char) 48120, (char) 51221})
                : value.trim();
    }

    private ConcertInfoVo parseJsonObjectResponse(String answer) {
        if (answer == null || answer.isBlank()) {
            return null;
        }

        String json = extractJsonPayload(answer);
        if (json.equals("{}") || json.equals("[]") || json.equals("-")) {
            return null;
        }

        try {
            return objectMapper.readValue(json, ConcertInfoVo.class);
        } catch (Exception e) {
            log.error("[GeminiClient] JSON object parsing failed. Raw response:\n{}", answer, e);
            return null;
        }
    }

    private String extractJsonPayload(String answer) {
        String json = answer.trim();
        if (json.startsWith("```")) {
            int firstNewline = json.indexOf('\n');
            int lastBacktick = json.lastIndexOf("```");
            if (firstNewline != -1 && lastBacktick > firstNewline) {
                json = json.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        return json;
    }

    private List<ConcertInfoVo> concertInfoReprocess(String answer) throws Exception {
        List<ConcertInfoVo> resultList = new ArrayList<>();

        if (answer == null || answer.isEmpty()) {
            return resultList;
        }

        String[] arrResult = answer.split("\n");

        for (String data : arrResult) {
            if (data == null) continue;
            data = data.trim();
            if (data.isEmpty()) continue;

            String[] tmp = data.split("/");

            // 최소한 아티스트와 날짜/시간은 있어야 함
            if (tmp.length < 2) continue;

            ConcertInfoVo vo = new ConcertInfoVo();

            // 0: 아티스트명
            vo.setArtistNmKor(tmp[0].trim());
            vo.setArtistNmFor(tmp[1].trim());

            // 1: 공연일시 yyyy-MM-dd HH:mm
            String dateTimeStr = tmp[2].trim();
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(
                        dateTimeStr,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                );
                vo.setConcertYear(dt.getYear());
                vo.setConcertMonth(dt.getMonthValue());
                vo.setConcertDate(dt.getDayOfMonth());
                vo.setConcertTime(String.format("%02d%02d", dt.getHour(), dt.getMinute()));
            } catch (Exception e) {
                log.error(dateTimeStr + "====날짜 파싱 실패");
            }

            // 2: 공연장소
            if (tmp.length > 3) {
                vo.setVenue(tmp[3].trim());
            }

            // 3: 예매처
            if (tmp.length > 4) {
                vo.setTctSite(tmp[4].trim());
            }

            // 4: 예매시간
            if (tmp.length > 5) {
                vo.setTctOpenAt(tmp[5].trim());
            }

            // 5: 장르
            if (tmp.length > 6) {
                vo.setGenre(tmp[6].trim());
            }

            // 6: 인기도 점수 (0~100 사이 INT 예상)
            if (tmp.length > 7) {
                try {
                    int score = Integer.parseInt(tmp[7].trim());
                    vo.setPopScore(score);
                } catch (NumberFormatException ignore) {
                    // 파싱 실패 시 기본값(0.0) 유지
                }
            }

            vo.setEmailYn("N");

            resultList.add(vo);
        }

        return resultList;
    }

    public ArtistMsgVo getArtistInfo(ConcertInfoVo concertInfoVo) {
        String artistNm = concertInfoVo.getArtistNmKor() + " (" + concertInfoVo.getArtistNmFor() + ")";

        String systemPrompt = loadPrompt("prompts/artist-info-system.txt");

        StringBuilder userSb = new StringBuilder();
        userSb.append("가수 ").append(artistNm).append("와 새로 예정된 공연에 대해서 1,000글자 이내로 소개하고 3개의 대표곡, 뽑은 대표곡들에 대한 설명도 덧 붙여줘.");

        Content systemInstruction = Content.builder()
                .parts(List.of(Part.builder().text(systemPrompt).build()))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(List.of(googleSearchTool))
                .systemInstruction(systemInstruction)
                .temperature(0.0f)
                .build();

        GenerateContentResponse response = client.models.generateContent(
                "gemini-3-pro-preview",
                Content.builder().parts(List.of(Part.builder().text(userSb.toString()).build())).build(),
                config
        );

        log.info(response.text());

        // 답변을 제목과 본문으로 재가공
        ArtistMsgVo artistMsgVo = this.artistMsgProcess(response.text());
        artistMsgVo.setArtistNmKor(concertInfoVo.getArtistNmKor());
        artistMsgVo.setArtistNmFor(concertInfoVo.getArtistNmFor());
        artistMsgVo.setConcertYear(concertInfoVo.getConcertYear());
        artistMsgVo.setConcertMonth(concertInfoVo.getConcertMonth());
        artistMsgVo.setConcertDate(concertInfoVo.getConcertDate());
        return artistMsgVo;
    }

    private ArtistMsgVo artistMsgProcess(String answer) {
        ArtistMsgVo artistMsgVo = new ArtistMsgVo();

        String subject = "";
        String content = "";
        int subjectIdx = answer.indexOf("subject-");
        if (subjectIdx != -1) {
            int lineEndIdx = answer.indexOf('\n', subjectIdx);
            if (lineEndIdx == -1) {
                lineEndIdx = answer.length();
            }

            // 'subject-' 이후부터 줄 끝까지를 제목으로 사용 (접두어는 제거)
            int titleStart = subjectIdx + "subject-".length();
            if (titleStart < lineEndIdx) {
                subject = answer.substring(titleStart, lineEndIdx).trim();
            }

            // 줄바꿈 뒤부터 끝까지를 본문으로 사용
            if (lineEndIdx < answer.length()) {
                content = answer.substring(lineEndIdx + 1).trim();
            }

            artistMsgVo.setSubject(subject);
            artistMsgVo.setContent(content);
        }

        return artistMsgVo;
    }

    public void getArtistPicture(ArtistMsgVo artistMsgVo) throws Exception {
        String artistNmFor = artistMsgVo.getArtistNmFor();

        List<String> imgUrls = getImageUrls(artistNmFor, 5);
        boolean saved = false;
        if (imgUrls != null) {
            for (String imgUrl : imgUrls) {
                if (imgUrl == null || !imgUrl.startsWith("http")) {
                    continue;
                }
                try {
                    String fileName = "artist_" + java.util.UUID.randomUUID();
                    String savedFileName = saveImageFromServer(imgUrl, fileName);
                    artistMsgVo.setArtistImagePath(Paths.get(ensureTrailingSlash(imageDir)).resolve(savedFileName).toAbsolutePath().normalize().toString());
                    artistMsgVo.setArtistImageUrl(buildPublicImageUrl("/concert_images/" + savedFileName));
                    System.out.println("저장 완료: " + ensureTrailingSlash(imageDir) + savedFileName);
                    saved = true;
                    break;
                } catch (IOException e) {
                    // 도메인 블랙 리스트 저장

                    log.warn("Image download failed, try next. url={}", imgUrl);
                }
            }
        }

        if (!saved) {
            System.out.println("이미지 URL을 찾지 못했습니다.");
        }
    }

    private List<String> getImageUrls(String query, int max) throws Exception {
        String encodedQuery = URLEncoder.encode("artist " + query + " concert", StandardCharsets.UTF_8);
        String safeApiKey = (serpApiKey != null) ? serpApiKey.trim() : "";

        String urlString = String.format(
                "https://serpapi.com/search.json?q=%s&tbm=isch&api_key=%s&num=%d",
                encodedQuery, safeApiKey, max
        );

        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        int responseCode = conn.getResponseCode();
        if (responseCode >= 400) {
            try (InputStream es = conn.getErrorStream();
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(es))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                log.error("SerpAPI Error (Code {}): {}", responseCode, errorResponse);
                return List.of();
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.has("images_results")) {
                List<String> result = new ArrayList<>();
                int count = jsonResponse.getJSONArray("images_results").length();
                int limit = Math.min(count, max);
                for (int i = 0; i < limit; i++) {
                    JSONObject item = jsonResponse.getJSONArray("images_results").getJSONObject(i);
                    if (item.has("original")) {
                        result.add(item.getString("original"));
                    }
                }
                return result;
            }
        }
        return List.of();
    }

    // URL의 이미지를 서버 파일로 저장 (저장 시점에 리사이징/재압축)
    public String saveImageFromServer(String imageUrl, String fileName) throws IOException {
        // 저장할 폴더가 없으면 생성
        String dirPath = ensureTrailingSlash(imageDir);
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dirPath);
        }

        URL url = URI.create(imageUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Referer", "https://www.google.com/");

        String contentType = connection.getContentType();

        // 원본 바이트를 메모리로 읽은 뒤 리사이징을 시도하고, 실패하면 원본을 그대로 저장한다.
        byte[] imageBytes;
        try (InputStream in = connection.getInputStream()) {
            imageBytes = in.readAllBytes();
        }

        try {
            String jpgFileName = fileName + ".jpg";
            resizeToJpeg(imageBytes, dirPath + jpgFileName);
            return jpgFileName;
        } catch (Exception e) {
            // webp/gif 등 디코딩 불가 입력은 원본을 그대로 저장 (fallback)
            log.warn("Image resize failed, save original. url={}, cause={}", imageUrl, e.toString());
            String ext = extFromContentType(contentType);
            if (ext == null) {
                ext = ".jpg";
            }
            String finalFileName = fileName + ext;
            Files.write(Paths.get(dirPath + finalFileName), imageBytes);
            return finalFileName;
        }
    }

    /**
     * 이미지 바이트를 폭/높이 최대 {@value #MAX_IMAGE_DIMENSION}px, JPEG 품질 {@value #JPEG_QUALITY}로
     * 리사이징하여 지정 경로에 저장한다. 비율은 유지된다. 디코딩 불가 입력이면 IOException을 던진다.
     */
    public static void resizeToJpeg(byte[] imageBytes, String targetPath) throws IOException {
        try (InputStream src = new ByteArrayInputStream(imageBytes)) {
            Thumbnails.of(src)
                    .size(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toFile(new File(targetPath));
        }
    }

    private String buildPublicImageUrl(String path) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return path;
        }
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return base + cleanPath;
    }

    private String extFromContentType(String contentType) {
        if (contentType == null) return null;
        String ct = contentType.toLowerCase();
        if (ct.contains("image/jpeg") || ct.contains("image/jpg")) return ".jpg";
        if (ct.contains("image/png")) return ".png";
        if (ct.contains("image/webp")) return ".webp";
        if (ct.contains("image/gif")) return ".gif";
        return null;
    }
    private String ensureTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "./data/concert_images/";
        }
        return value.endsWith("/") || value.endsWith("\\") ? value : value + "/";
    }

    private String loadPrompt(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException("프롬프트 파일 로드 실패: " + path, e);
        }
    }
}


