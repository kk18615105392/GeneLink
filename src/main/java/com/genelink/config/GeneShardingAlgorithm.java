package com.genelink.config;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * 自定义 ShardingSphere 基因分片算法：
 * 从 shortCode 末位基因字符计算物理表下标，实现精准路由、避免广播查询。
 */
public class GeneShardingAlgorithm implements StandardShardingAlgorithm<String> {

    private Properties props;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<String> shardingValue) {
        String shortCode = shardingValue.getValue();
        if (shortCode == null || shortCode.isEmpty()) {
            throw new IllegalArgumentException("Sharding value (shortCode) cannot be empty");
        }

        char geneChar = shortCode.charAt(shortCode.length() - 1);
        int shardIndex = geneChar % availableTargetNames.size();
        String logicTableName = shardingValue.getLogicTableName();
        String targetTable = logicTableName + "_" + shardIndex;

        if (availableTargetNames.contains(targetTable)) {
            return targetTable;
        }
        throw new UnsupportedOperationException("Cannot find target table path: " + targetTable);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<String> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    @Override
    public String getType() {
        return "CLASS_BASED";
    }
}
