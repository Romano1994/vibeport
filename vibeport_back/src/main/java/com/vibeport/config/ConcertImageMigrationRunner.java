package com.vibeport.config;

import com.vibeport.ai.client.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * 기존에 리사이징 없이 저장된 큰 아티스트 이미지를 일회성으로 재압축한다.
 * {@code app.image-migration.enabled=true}일 때만 동작하며, 1회 실행 후 다시 false로 되돌려야 한다.
 * 파일명을 그대로 유지하므로 DB({@code TB_ARTIST_MSG.ARTIST_IMAGE_URL})는 변경하지 않는다.
 */
@Slf4j
@Component
public class ConcertImageMigrationRunner implements ApplicationRunner {

    // 이 크기를 초과하는 파일만 재압축 대상으로 삼는다 (이미 작은 파일은 건너뜀).
    private static final long SIZE_THRESHOLD_BYTES = 500L * 1024L;

    @Value("${app.image-migration.enabled:false}")
    private boolean migrationEnabled;

    @Value("${app.image-dir:./data/concert_images/}")
    private String imageDir;

    @Override
    public void run(ApplicationArguments args) {
        if (!migrationEnabled) {
            return;
        }

        String dirPath = imageDir.endsWith("/") || imageDir.endsWith("\\") ? imageDir : imageDir + "/";
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null) {
            log.warn("[image-migration] 디렉토리를 찾을 수 없습니다: {}", dir.getAbsolutePath());
            return;
        }

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        long savedBytes = 0L;

        for (File file : files) {
            if (!file.isFile() || !isResizableImage(file.getName())) {
                continue;
            }
            long originalSize = file.length();
            if (originalSize <= SIZE_THRESHOLD_BYTES) {
                skipped++;
                continue;
            }

            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                Path tmp = Paths.get(dirPath + file.getName() + ".tmp");
                GeminiClient.resizeToJpeg(bytes, tmp.toString());

                long newSize = Files.size(tmp);
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                savedBytes += (originalSize - newSize);
                processed++;
                log.info("[image-migration] 재압축: {} ({} bytes -> {} bytes)", file.getName(), originalSize, newSize);
            } catch (IOException e) {
                failed++;
                log.warn("[image-migration] 재압축 실패(건너뜀): {}, cause={}", file.getName(), e.toString());
            }
        }

        log.info("[image-migration] 완료. 재압축 {}건, 건너뜀 {}건, 실패 {}건, 절감 {} KB. 작업 후 app.image-migration.enabled를 false로 되돌리세요.",
                processed, skipped, failed, savedBytes / 1024L);
    }

    private boolean isResizableImage(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }
}
