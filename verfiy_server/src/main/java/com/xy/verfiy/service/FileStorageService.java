package com.xy.verfiy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 文件存储服务
 * 负责管理 Dex 和 Zip 文件的存储、读取和删除
 */
@Service
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    @Value("${file.storage.base-path:/data/verfiy}")
    private String basePath;
    
    private static final String DEX_DIR = "dex";
    private static final String ZIP_DIR = "resources";
    
    /**
     * 初始化存储目录
     */
    private void initStorageDirectories() throws IOException {
        Path dexPath = Paths.get(basePath, DEX_DIR);
        Path zipPath = Paths.get(basePath, ZIP_DIR);
        
        if (!Files.exists(dexPath)) {
            Files.createDirectories(dexPath);
            logger.info("创建 Dex 存储目录: {}", dexPath);
        }
        
        if (!Files.exists(zipPath)) {
            Files.createDirectories(zipPath);
            logger.info("创建 Zip 存储目录: {}", zipPath);
        }
    }
    
    /**
     * 计算文件的 SHA-256 哈希值
     */
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
    
    /**
     * 保存 Dex 文件
     * @param data Dex 文件的字节数组
     * @return 文件存储路径（相对于 basePath）
     */
    public String saveDexFile(byte[] data) throws IOException {
        initStorageDirectories();
        
        String hash = calculateHash(data);
        String filename = hash + ".dex";
        String relativePath = DEX_DIR + "/" + filename;
        Path fullPath = Paths.get(basePath, relativePath);
        
        // 如果文件已存在，不重复保存（去重）
        if (Files.exists(fullPath)) {
            logger.info("Dex 文件已存在，跳过保存: {}", relativePath);
            return relativePath;
        }
        
        Files.write(fullPath, data);
        logger.info("保存 Dex 文件: {} ({}KB)", relativePath, data.length / 1024);
        
        return relativePath;
    }
    
    /**
     * 保存 Zip 文件
     * @param data Zip 文件的字节数组
     * @param version 资源版本号
     * @return 文件存储路径（相对于 basePath）
     */
    public String saveZipFile(byte[] data, int version) throws IOException {
        initStorageDirectories();
        
        String hash = calculateHash(data);
        String filename = hash + "_v" + version + ".zip";
        String relativePath = ZIP_DIR + "/" + filename;
        Path fullPath = Paths.get(basePath, relativePath);
        
        // 如果文件已存在，不重复保存（去重）
        if (Files.exists(fullPath)) {
            logger.info("Zip 文件已存在，跳过保存: {}", relativePath);
            return relativePath;
        }
        
        Files.write(fullPath, data);
        logger.info("保存 Zip 文件: {} ({}KB)", relativePath, data.length / 1024);
        
        return relativePath;
    }
    
    /**
     * 读取 Dex 文件
     * @param relativePath 相对路径（从数据库读取的路径）
     * @return 文件字节数组
     */
    public byte[] loadDexFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        
        Path fullPath = Paths.get(basePath, relativePath);
        
        if (!Files.exists(fullPath)) {
            logger.warn("Dex 文件不存在: {}", fullPath);
            return null;
        }
        
        byte[] data = Files.readAllBytes(fullPath);
        logger.debug("读取 Dex 文件: {} ({}KB)", relativePath, data.length / 1024);
        
        return data;
    }
    
    /**
     * 读取 Zip 文件
     * @param relativePath 相对路径（从数据库读取的路径）
     * @return 文件字节数组
     */
    public byte[] loadZipFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        
        Path fullPath = Paths.get(basePath, relativePath);
        
        if (!Files.exists(fullPath)) {
            logger.warn("Zip 文件不存在: {}", fullPath);
            return null;
        }
        
        byte[] data = Files.readAllBytes(fullPath);
        logger.debug("读取 Zip 文件: {} ({}KB)", relativePath, data.length / 1024);
        
        return data;
    }
    
    /**
     * 删除 Dex 文件
     * @param relativePath 相对路径
     */
    public void deleteDexFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        
        try {
            Path fullPath = Paths.get(basePath, relativePath);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                logger.info("删除 Dex 文件: {}", relativePath);
            }
        } catch (IOException e) {
            logger.error("删除 Dex 文件失败: {}", relativePath, e);
        }
    }
    
    /**
     * 删除 Zip 文件
     * @param relativePath 相对路径
     */
    public void deleteZipFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        
        try {
            Path fullPath = Paths.get(basePath, relativePath);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                logger.info("删除 Zip 文件: {}", relativePath);
            }
        } catch (IOException e) {
            logger.error("删除 Zip 文件失败: {}", relativePath, e);
        }
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        
        Path fullPath = Paths.get(basePath, relativePath);
        return Files.exists(fullPath);
    }
}

