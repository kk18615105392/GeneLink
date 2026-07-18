package com.genelink.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LinkResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private String gid;
    private Character geneChar;
    private Integer shardIndex;
    private LocalDateTime createTime;
}
