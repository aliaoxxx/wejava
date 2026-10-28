package com.wejava.system.model.vo;

import lombok.Data;

/**
 * 服务监控信息。字段扁平化，避免为一堆小对象各建一个文件。
 */
@Data
public class ServerInfoVO {

    // 操作系统
    private String osName;
    private String osArch;
    private String osVersion;
    private int cpuCores;
    private double cpuLoad;
    private long memTotal;
    private long memUsed;

    // JVM
    private String javaVersion;
    private String jvmName;
    private long jvmTotal;
    private long jvmUsed;
    private long jvmMax;
    private long uptimeSeconds;

    // 磁盘
    private String diskPath;
    private long diskTotal;
    private long diskUsable;

    // 数据库 / 缓存
    private String dbProduct;
    private String dbVersion;
    private String redisVersion;
    private String redisUsedMemory;
}
