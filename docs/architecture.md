# GeneLink 架构说明

## 五技落点

| 技术点 | 代码位置 | 作用 |
|--------|----------|------|
| 基因分片法 | `ShortLinkService#createShortLink` | GID 哈希取模得到基因字符，拼到 shortCode 末位 |
| ShardingSphere CLASS_BASED | `GeneShardingAlgorithm` + `sharding.yaml` | 查询时从 shortCode 末位路由到 `link_mapping_0/1` |
| 多级缓存 | `ShortLinkService#getOriginalUrl` | Caffeine → Redis → DB |
| DCL 防击穿 | `ShortLinkService` synchronized 双检 | 热点失效时只放行一个线程打 DB |
| 号段模式 ID | `SequenceGenerator` | Redis `INCRBY` 批量取号，本地 CAS 分配 |

## 写入路径

1. `SequenceGenerator.nextId()` 取全局唯一序列
2. Base62 编码序列
3. `geneChar = CHARACTERS[|gid.hashCode()| % 62]`
4. `shortCode = base62 + geneChar`
5. JPA 写入逻辑表 `link_mapping`（由 ShardingSphere 路由到物理表）
6. 预热 L1/L2 缓存

## 读取路径

1. L1 Caffeine
2. L2 Redis（命中则回填 L1）
3. Miss 时 DCL，查 DB，回写缓存；不存在则缓存空值 placeholder 防穿透

## 为什么基因要进 shortCode

短链跳转请求通常只有 shortCode，没有 gid。若分片键是 gid，查询只能广播所有分片。把 gid 的「基因」编码进 shortCode，即可单分片精准命中。
