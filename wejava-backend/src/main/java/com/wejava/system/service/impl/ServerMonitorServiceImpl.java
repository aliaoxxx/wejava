package com.wejava.system.service.impl;

import com.wejava.system.model.vo.ServerInfoVO;
import com.wejava.system.service.ServerMonitorService;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerMonitorServiceImpl implements ServerMonitorService {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    @Override
    public ServerInfoVO info() {
        ServerInfoVO vo = new ServerInfoVO();
        fillOs(vo);
        fillJvm(vo);
        fillDisk(vo);
        fillDb(vo);
        fillRedis(vo);
        return vo;
    }

    private void fillOs(ServerInfoVO vo) {
        vo.setOsName(System.getProperty("os.name"));
        vo.setOsArch(System.getProperty("os.arch"));
        vo.setOsVersion(System.getProperty("os.version"));
        vo.setCpuCores(Runtime.getRuntime().availableProcessors());
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean sun) {
                vo.setCpuLoad(round(sun.getCpuLoad()));
                vo.setMemTotal(sun.getTotalMemorySize());
                vo.setMemUsed(sun.getTotalMemorySize() - sun.getFreeMemorySize());
            }
        } catch (Throwable e) {
            log.debug("读取系统指标失败: {}", e.getMessage());
        }
    }

    private void fillJvm(ServerInfoVO vo) {
        Runtime runtime = Runtime.getRuntime();
        vo.setJavaVersion(System.getProperty("java.version"));
        vo.setJvmName(System.getProperty("java.vm.name"));
        vo.setJvmTotal(runtime.totalMemory());
        vo.setJvmUsed(runtime.totalMemory() - runtime.freeMemory());
        vo.setJvmMax(runtime.maxMemory());
        vo.setUptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
    }

    private void fillDisk(ServerInfoVO vo) {
        try {
            Path path = Paths.get(new File(".").getAbsolutePath());
            FileStore store = Files.getFileStore(path);
            vo.setDiskPath(path.toAbsolutePath().normalize().toString());
            vo.setDiskTotal(store.getTotalSpace());
            vo.setDiskUsable(store.getUsableSpace());
        } catch (Exception e) {
            log.debug("读取磁盘指标失败: {}", e.getMessage());
        }
    }

    private void fillDb(ServerInfoVO vo) {
        try (var connection = dataSource.getConnection()) {
            var meta = connection.getMetaData();
            vo.setDbProduct(meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            vo.setDbVersion(String.valueOf(meta.getDatabaseMajorVersion()));
        } catch (Exception e) {
            log.debug("读取数据库指标失败: {}", e.getMessage());
        }
    }

    private void fillRedis(ServerInfoVO vo) {
        try {
            Properties server = redisTemplate.execute((RedisCallback<Properties>) RedisConnection::info);
            if (server != null) {
                vo.setRedisVersion(server.getProperty("redis_version"));
                vo.setRedisUsedMemory(server.getProperty("used_memory_human"));
            }
        } catch (Exception e) {
            log.debug("读取 Redis 指标失败: {}", e.getMessage());
        }
    }

    private double round(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0;
        }
        return Math.round(value * 10000d) / 10000d;
    }
}
