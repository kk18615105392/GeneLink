# GeneLink（基因链）

> 基于 **Spring Boot 3 + Apache ShardingSphere 5.4.1** 的高性能短链接平台。  
> 用「基因分片法」把分组信息写入短码，查询时精准路由到分片表；配合多级缓存、DCL 防击穿与 Redis 号段发号，演示生产级短链读写链路。

[![CI](https://github.com/kk18615105392/GeneLink/actions/workflows/ci.yml/badge.svg)](https://github.com/kk18615105392/GeneLink/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0.12-brightgreen)](https://spring.io/projects/spring-boot)
[![ShardingSphere](https://img.shields.io/badge/ShardingSphere-5.4.1-blue)](https://shardingsphere.apache.org/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE)

---

## 这个项目是做什么的？

把长链接收成短码，访问短码时 **302 跳转**回原地址。

| 场景 | 示例 |
|------|------|
| 长链接 | `https://www.example.com/a/very/long/path?utm=...` |
| 短链接 | `http://localhost:8080/wj3` |
| 效果 | 打开短链 → 自动跳到长链 |

本地默认使用 **H2 内存库 + Embedded Redis**，克隆后即可跑测试、起服务、打开管理台，无需先装 MySQL / Redis。

---

## 核心亮点（面试可讲）

| 技术点 | 实现方式 | 价值 |
|--------|----------|------|
| **基因分片法** | GID 哈希注入 `shortCode` 末位 | 查询无需广播，精准路由分片 |
| **多级缓存** | Caffeine（L1）+ Redis（L2）+ DB（L3） | 扛短链跳转的读多写少流量 |
| **DCL 防击穿** | Double-Checked Locking + `synchronized` | 热点失效时只放行一个线程打 DB |
| **号段模式 ID** | Redis `INCRBY` 批量申请号段 | 高并发发号，降低 Redis 压力 |
| **ShardingSphere JDBC** | `CLASS_BASED` 自定义分片算法 | 逻辑表 → 物理表自动路由 |

### 基因分片在做什么？

```text
写入：
  gid = "user_group_1"
  geneChar = CHARACTERS[|gid.hashCode()| % 62]   → 例如 '3'
  shortCode = Base62(sequenceId) + geneChar      → 例如 "wj3"
                                                      ↑ 基因位

查询：
  shortCode = "wj3"
  geneChar  = 末位 '3'
  shardIndex = '3' % 2
  → 直接访问 link_mapping_0 或 link_mapping_1（无需全表广播）
```

更完整的架构说明见：[docs/architecture.md](docs/architecture.md)

---

## 环境要求

- **JDK 17+**
- **Maven 3.9+**
- 操作系统：Windows / macOS / Linux（Windows 上若 Embedded Redis 启动失败，请本机安装 Redis 并监听 `6379`）

---

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/kk18615105392/GeneLink.git
cd GeneLink
```

### 2. 跑测试（推荐先跑）

```bash
mvn -s settings.xml test
```

预期输出包含：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖内容：

| 测试 | 说明 |
|------|------|
| `testSequenceGeneratorConcurrency` | 100 线程 × 50 次发号，验证无重复 |
| `testMultilevelCacheAndDCL` | 创建短链后 50 线程并发读取 |
| `testCachePenetrationAndDCL` | 30 线程并发查不存在短码，验证防穿透 |

### 3. 启动服务

```bash
mvn -s settings.xml spring-boot:run
```

看到 `Started GeneLinkApplication` 即表示成功。

### 4. 打开管理台体验

浏览器访问：**http://localhost:8080/**

1. 填写长链接与分组 GID  
2. 点击 **生成短链**  
3. 复制短链或点 **打开跳转** 验证 302  
4. 可点 **健康检查** 查看缓存 / Redis / 分片状态  

---

## API 手册

统一响应格式：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

`code = 0` 表示成功，非 0 表示业务/参数错误。

### 创建短链

```http
POST /api/v1/links
Content-Type: application/json

{
  "originalUrl": "https://www.example.com",
  "gid": "user_group_1"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "shortCode": "wj3",
    "shortUrl": "http://localhost:8080/wj3",
    "originalUrl": "https://www.example.com",
    "gid": "user_group_1",
    "geneChar": "3",
    "shardIndex": 1,
    "createTime": null
  }
}
```

字段说明：

| 字段 | 含义 |
|------|------|
| `shortCode` | 短码（含末位基因字符） |
| `shortUrl` | 可直接访问的短链地址 |
| `geneChar` | 注入的基因字符 |
| `shardIndex` | 路由到的物理分片下标（0 / 1） |

curl 示例：

```bash
curl -X POST http://localhost:8080/api/v1/links \
  -H "Content-Type: application/json" \
  -d "{\"originalUrl\":\"https://www.example.com\",\"gid\":\"user_group_1\"}"
```

PowerShell 示例：

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/links `
  -ContentType 'application/json' `
  -Body '{"originalUrl":"https://www.example.com","gid":"user_group_1"}'
```

### 查询短链元数据

```http
GET /api/v1/links/{shortCode}
```

### 短链跳转

```http
GET /{shortCode}
```

成功时返回 **302**，`Location` 指向原始长链接。

### 健康检查

```http
GET /api/v1/health
```

返回应用状态、L1 缓存规模、Redis ping、分片表列表等。

---

## 系统架构

```text
浏览器 / API 客户端
        │
        ▼
┌───────────────────┐
│  ShortLinkController │  创建 / 查询 / 302 跳转
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│  ShortLinkService │
│  L1 Caffeine      │
│  L2 Redis         │
│  DCL 防击穿       │
└─────────┬─────────┘
          │ miss
          ▼
┌───────────────────┐
│ ShardingSphere    │  GeneShardingAlgorithm
│ (Gene Routing)    │
└─────────┬─────────┘
     ┌────┴────┐
     ▼         ▼
link_mapping_0  link_mapping_1
```

号段发号：`SequenceGenerator` ← Redis `INCRBY`（批量步长 1000）→ 本地 CAS 分配。

---

## 项目结构

```text
GeneLink/
├── README.md
├── pom.xml
├── settings.xml                      # 国内 Maven 镜像（阿里云 / 华为云）
├── docs/
│   └── architecture.md               # 基因分片 / 缓存 / 号段详解
└── src/
    ├── main/java/com/genelink/
    │   ├── GeneLinkApplication.java
    │   ├── config/
    │   │   ├── GeneShardingAlgorithm.java   # ★ 基因分片算法
    │   │   ├── CaffeineCacheConfig.java
    │   │   ├── EmbeddedRedisConfig.java
    │   │   ├── DatabaseInitializer.java     # 物理表预创建（启动前）
    │   │   └── H2TableContextInitializer.java
    │   ├── controller/                      # REST + 跳转 + 健康检查
    │   ├── service/ShortLinkService.java    # ★ 基因注入 + 多级缓存 + DCL
    │   ├── generator/SequenceGenerator.java # ★ 号段 ID
    │   ├── entity/ / repository/ / util/ / web/
    │   └── ...
    ├── main/resources/
    │   ├── application.yml
    │   ├── config/sharding.yaml             # ★ ShardingSphere 规则
    │   └── static/index.html                # 管理台
    └── test/java/.../ShortLinkConcurrencyTest.java
```

---

## 关键代码索引（对着讲）

| 你想讲的点 | 去看这里 |
|------------|----------|
| 基因注入短码 | `ShortLinkService#createShortLink` |
| 多级缓存 + DCL + 空值防穿透 | `ShortLinkService#getOriginalUrl` |
| 自定义分片路由 | `GeneShardingAlgorithm` |
| 分片规则配置 | `src/main/resources/config/sharding.yaml` |
| 号段发号 | `SequenceGenerator` |
| 物理表必须在 SS 启动前建好 | `DatabaseInitializer` + `H2TableContextInitializer` |

---

## 常见问题

**Q: 报错 `Table or view link_mapping does not exist`？**  
A: 请使用本仓库最新代码。物理表会在 Spring / ShardingSphere 初始化**之前**创建；不要删掉 `H2TableContextInitializer` / `main` 里的预建表逻辑。

**Q: Embedded Redis 启动失败？**  
A: Windows 上偶发。若本机已有 Redis 占用 `6379`，一般可直接复用；否则请安装 Redis 并启动在 `6379`。号段发号与 L2 缓存依赖 Redis。

**Q: 端口 8080 被占用？**  
A: 修改 `src/main/resources/application.yml` 中的 `server.port`。

**Q: 想换成 MySQL？**  
A: 创建物理表 `link_mapping_0` / `link_mapping_1`，并修改 `sharding.yaml` 的数据源为 MySQL JDBC（H2 仅用于本地零依赖演示）。

---

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 语言 / 构建 | Java / Maven | 17 / 3.9+ |
| 框架 | Spring Boot | 3.0.12 |
| ORM | Spring Data JPA / Hibernate | 6.x |
| 分片 | Apache ShardingSphere JDBC | 5.4.1 |
| 缓存 | Caffeine + Spring Data Redis | - |
| 本地库 | H2（MySQL 模式） | - |
| 可切换生产库 | MySQL | 8.0+ |

---

## License

MIT

---

_作者：崔泽坤 · GeneLink · 2026_
