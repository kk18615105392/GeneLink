package com.genelink.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLinkRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 1024, message = "最长 1024 字符")
    private String originalUrl;

    @NotBlank(message = "不能为空")
    @Size(max = 32, message = "最长 32 字符")
    private String gid;
}
