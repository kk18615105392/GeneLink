package com.genelink.controller;

import com.genelink.controller.dto.CreateLinkRequest;
import com.genelink.controller.dto.LinkResponse;
import com.genelink.entity.LinkMapping;
import com.genelink.service.ShortLinkService;
import com.genelink.web.ApiResponse;
import com.genelink.web.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class ShortLinkController {

    private static final int SHARD_COUNT = 2;

    @Autowired
    private ShortLinkService shortLinkService;

    @Value("${genelink.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping("/api/v1/links")
    public ApiResponse<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        String shortCode = shortLinkService.createShortLink(request.getOriginalUrl().trim(), request.getGid().trim());
        return ApiResponse.ok(toResponse(shortCode, request.getOriginalUrl().trim(), request.getGid().trim(), null));
    }

    @GetMapping("/api/v1/links/{shortCode}")
    public ApiResponse<LinkResponse> get(@PathVariable String shortCode) {
        LinkMapping mapping = shortLinkService.findByShortCode(shortCode)
                .orElseThrow(() -> new BusinessException(404, "短链不存在: " + shortCode));
        return ApiResponse.ok(toResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getGid(),
                mapping.getCreateTime()
        ));
    }

    /** 仅匹配 Base62 短码，避免抢走静态资源与 API 路径 */
    @GetMapping("/{shortCode:[0-9A-Za-z]+}")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response) throws IOException {
        String originalUrl = shortLinkService.getOriginalUrl(shortCode);
        if (originalUrl != null) {
            response.sendRedirect(originalUrl);
        } else {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Short link not found");
        }
    }

    private LinkResponse toResponse(String shortCode, String originalUrl, String gid, java.time.LocalDateTime createTime) {
        char geneChar = shortCode.charAt(shortCode.length() - 1);
        int shardIndex = geneChar % SHARD_COUNT;
        return LinkResponse.builder()
                .shortCode(shortCode)
                .shortUrl(baseUrl.replaceAll("/$", "") + "/" + shortCode)
                .originalUrl(originalUrl)
                .gid(gid)
                .geneChar(geneChar)
                .shardIndex(shardIndex)
                .createTime(createTime)
                .build();
    }
}
