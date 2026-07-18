package com.genelink.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "link_mapping", indexes = {
        @Index(name = "idx_short_code", columnList = "short_code", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 1024)
    private String originalUrl;

    @Column(name = "gid", nullable = false, length = 32)
    private String gid;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
