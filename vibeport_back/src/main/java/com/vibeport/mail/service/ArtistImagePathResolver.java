package com.vibeport.mail.service;

import com.vibeport.ai.vo.ArtistMsgVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class ArtistImagePathResolver {

    private static final String PUBLIC_IMAGE_SEGMENT = "/concert_images/";

    @Value("${app.image-dir:./data/concert_images/}")
    private String imageDir;

    public String resolveInlineImagePath(ArtistMsgVo letterVo) {
        if (letterVo == null) {
            return null;
        }

        String directPath = letterVo.getArtistImagePath();
        if (directPath != null && !directPath.isBlank() && Files.exists(Paths.get(directPath))) {
            return directPath;
        }

        String url = letterVo.getArtistImageUrl();
        if (url == null || url.isBlank()) {
            return null;
        }

        int segIdx = url.lastIndexOf(PUBLIC_IMAGE_SEGMENT);
        if (segIdx < 0) {
            log.warn("Inline image skipped - URL is not under {} : {}", PUBLIC_IMAGE_SEGMENT, url);
            return null;
        }

        String fileName = url.substring(segIdx + PUBLIC_IMAGE_SEGMENT.length());
        int queryIdx = fileName.indexOf('?');
        if (queryIdx >= 0) {
            fileName = fileName.substring(0, queryIdx);
        }
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            log.warn("Inline image skipped - suspicious file name extracted from URL: {}", url);
            return null;
        }

        Path resolved = Paths.get(ensureTrailingSlash(imageDir)).resolve(fileName).toAbsolutePath().normalize();
        if (!Files.exists(resolved)) {
            log.warn("Inline image skipped - local cache miss. url={}, path={}", url, resolved);
            return null;
        }
        return resolved.toString();
    }

    private String ensureTrailingSlash(String dir) {
        if (dir == null || dir.isBlank()) {
            return "./data/concert_images/";
        }
        return (dir.endsWith("/") || dir.endsWith("\\")) ? dir : dir + "/";
    }
}
