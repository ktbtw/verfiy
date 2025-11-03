package com.xy.verfiy.service;

import com.xy.verfiy.domain.DexCompileTask;
import com.xy.verfiy.mapper.DexCompileTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DexCompileService {

    @Autowired
    private DexCompileTaskMapper dexCompileTaskMapper;

    @Value("${dex.compile.android-home}")
    private String androidHome;

    @Value("${dex.compile.build-tools-version}")
    private String buildToolsVersion;

    @Value("${dex.compile.jar-lib-path}")
    private String jarLibPath;

    @Value("${dex.compile.temp-dir}")
    private String tempDirBase;

    @Value("${dex.compile.timeout:60}")
    private int timeoutSeconds;

    /**
     * 编译多个 Java 文件为 Dex
     */
    public DexCompileTask compileMultipleFilesToDex(Map<String, String> files, Long userId) {
        String taskId = UUID.randomUUID().toString();
        DexCompileTask task = new DexCompileTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setJavaCode(files.size() + " 个文件");
        task.setSuccess(false);
        task.setDownloaded(false);

        Path workDir = null;
        StringBuilder log = new StringBuilder();

        try {
            // 创建工作目录
            workDir = createWorkDirectory(taskId);
            log.append("工作目录: ").append(workDir).append("\n");
            log.append("文件数量: ").append(files.size()).append("\n");

            // 创建源文件目录
            Path srcDir = workDir.resolve("src");
            Files.createDirectories(srcDir);

            // 写入所有 Java 文件
            log.append("\n=== 写入文件 ===\n");
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String relativePath = entry.getKey();
                String content = entry.getValue();
                
                Path javaFile = srcDir.resolve(relativePath);
                Files.createDirectories(javaFile.getParent());
                Files.writeString(javaFile, content);
                log.append("  ").append(relativePath).append("\n");
            }

            // 创建输出目录
            Path classesDir = workDir.resolve("classes");
            Path dexDir = workDir.resolve("dex");
            Files.createDirectories(classesDir);
            Files.createDirectories(dexDir);

            // 步骤 1: 编译所有 Java 文件 -> Class
            log.append("\n=== 编译 Java -> Class ===\n");
            String javacLog = compileMultipleJavaToClass(srcDir, classesDir);
            log.append(javacLog);

            // 检查是否生成了 .class 文件
            if (!hasClassFiles(classesDir)) {
                throw new RuntimeException("编译失败：未生成 .class 文件");
            }

            // 步骤 2: 转换 Class -> Dex
            log.append("\n=== 转换 Class -> Dex ===\n");
            Path dexFile = dexDir.resolve("classes.dex");
            String d8Log = convertClassToDex(classesDir, dexFile);
            log.append(d8Log);

            // 检查是否生成了 .dex 文件
            if (!Files.exists(dexFile) || Files.size(dexFile) == 0) {
                throw new RuntimeException("Dex 转换失败：未生成 classes.dex 文件");
            }

            log.append("\n=== 编译成功 ===\n");
            log.append("Dex 文件: ").append(dexFile).append("\n");
            log.append("大小: ").append(Files.size(dexFile)).append(" bytes\n");

            // 保存任务信息
            task.setSuccess(true);
            task.setDexFilePath(dexFile.toString());
            task.setCompileLog(log.toString());
            dexCompileTaskMapper.insert(task);

            DexCompileService.log.info("编译成功，任务ID: {}", taskId);
            return task;

        } catch (Exception e) {
            log.append("\n=== 编译失败 ===\n");
            log.append(getStackTrace(e));

            task.setSuccess(false);
            task.setCompileLog(log.toString());
            dexCompileTaskMapper.insert(task);

            // 清理工作目录
            if (workDir != null) {
                try {
                    deleteDirectory(workDir);
                } catch (Exception cleanEx) {
                    DexCompileService.log.warn("清理工作目录失败: {}", cleanEx.getMessage());
                }
            }

            DexCompileService.log.error("编译失败", e);
            return task;
        }
    }

    /**
     * 编译单个 Java 文件为 Dex（向后兼容）
     */
    public DexCompileTask compileJavaToDex(String javaCode, Long userId) {
        // 转换为多文件格式
        String packageName = extractPackageName(javaCode);
        String className = extractClassName(javaCode);
        String filePath = packageName.replace('.', '/') + "/" + className + ".java";
        
        Map<String, String> files = new HashMap<>();
        files.put(filePath, javaCode);
        
        return compileMultipleFilesToDex(files, userId);
    }

    /**
     * 旧的单文件编译方法（已废弃，仅保留以防万一）
     */
    @Deprecated
    public DexCompileTask compileJavaToDexOld(String javaCode) {
        String taskId = UUID.randomUUID().toString();
        DexCompileTask task = new DexCompileTask();
        task.setTaskId(taskId);
        task.setJavaCode(javaCode);
        task.setSuccess(false);
        task.setDownloaded(false);

        Path workDir = null;
        StringBuilder log = new StringBuilder();

        try {
            // 创建工作目录
            workDir = createWorkDirectory(taskId);
            log.append("工作目录: ").append(workDir).append("\n");

            // 提取包名和类名
            String packageName = extractPackageName(javaCode);
            String className = extractClassName(javaCode);
            log.append("包名: ").append(packageName).append("\n");
            log.append("类名: ").append(className).append("\n");

            // 创建源文件目录结构
            Path srcDir = workDir.resolve("src");
            Path packageDir = srcDir;
            if (packageName != null && !packageName.isEmpty()) {
                packageDir = srcDir.resolve(packageName.replace('.', '/'));
            }
            Files.createDirectories(packageDir);

            // 写入 Java 文件
            Path javaFile = packageDir.resolve(className + ".java");
            Files.writeString(javaFile, javaCode);
            log.append("Java 文件: ").append(javaFile).append("\n");

            // 创建输出目录
            Path classesDir = workDir.resolve("classes");
            Path dexDir = workDir.resolve("dex");
            Files.createDirectories(classesDir);
            Files.createDirectories(dexDir);

            // 步骤 1: 编译 Java -> Class
            log.append("\n=== 编译 Java -> Class ===\n");
            String javacLog = compileJavaToClass(javaFile, classesDir);
            log.append(javacLog);

            // 检查是否生成了 .class 文件
            if (!hasClassFiles(classesDir)) {
                throw new RuntimeException("编译失败：未生成 .class 文件");
            }

            // 步骤 2: 转换 Class -> Dex
            log.append("\n=== 转换 Class -> Dex ===\n");
            Path dexFile = dexDir.resolve("classes.dex");
            String d8Log = convertClassToDex(classesDir, dexFile);
            log.append(d8Log);

            // 检查是否生成了 .dex 文件
            if (!Files.exists(dexFile) || Files.size(dexFile) == 0) {
                throw new RuntimeException("Dex 转换失败：未生成 classes.dex 文件");
            }

            // 成功
            task.setSuccess(true);
            task.setDexFilePath(dexFile.toString());
            task.setCompileLog(log.toString());
            log.append("\n✓ 编译成功！Dex 文件大小: ").append(Files.size(dexFile)).append(" bytes\n");

        } catch (Exception e) {
            log.append("\n✗ 编译失败: ").append(e.getMessage()).append("\n");
            log.append("详细错误: ").append(getStackTrace(e)).append("\n");
            task.setSuccess(false);
            task.setCompileLog(log.toString());
            DexCompileService.log.error("编译失败", e);
        }

        // 保存任务到数据库
        task.setCompileLog(log.toString());
        dexCompileTaskMapper.insert(task);

        return task;
    }

    /**
     * 获取编译任务
     */
    public DexCompileTask getTask(String taskId) {
        return dexCompileTaskMapper.findByTaskId(taskId);
    }

    /**
     * 获取 Dex 文件内容
     */
    public byte[] getDexFileBytes(String taskId) throws IOException {
        DexCompileTask task = dexCompileTaskMapper.findByTaskId(taskId);
        if (task == null) {
            throw new FileNotFoundException("任务不存在");
        }
        if (!task.getSuccess()) {
            throw new IllegalStateException("编译未成功，无法下载");
        }
        if (task.getDownloaded()) {
            throw new IllegalStateException("Dex 文件已被下载");
        }

        Path dexFile = Paths.get(task.getDexFilePath());
        if (!Files.exists(dexFile)) {
            throw new FileNotFoundException("Dex 文件不存在");
        }

        return Files.readAllBytes(dexFile);
    }

    /**
     * 标记为已下载并删除文件
     */
    public void markAsDownloaded(String taskId) throws IOException {
        DexCompileTask task = dexCompileTaskMapper.findByTaskId(taskId);
        if (task != null && task.getDexFilePath() != null) {
            // 标记为已下载
            dexCompileTaskMapper.updateDownloaded(taskId);

            // 删除工作目录
            Path dexFile = Paths.get(task.getDexFilePath());
            Path workDir = dexFile.getParent().getParent(); // dex/classes.dex -> taskId
            deleteDirectory(workDir);
        }
    }

    /**
     * 创建工作目录
     */
    private Path createWorkDirectory(String taskId) throws IOException {
        Path baseDir = Paths.get(tempDirBase);
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }
        Path workDir = baseDir.resolve(taskId);
        Files.createDirectories(workDir);
        return workDir;
    }

    /**
     * 编译多个 Java 文件 -> Class
     */
    private String compileMultipleJavaToClass(Path srcDir, Path outputDir) throws IOException, InterruptedException {
        // 构建 classpath
        List<String> classpath = new ArrayList<>();
        classpath.add(androidHome + "/platforms/android-34/android.jar");
        
        // 添加 jar_lib 目录下的所有 jar
        File jarLibDir = new File(jarLibPath);
        if (jarLibDir.exists() && jarLibDir.isDirectory()) {
            File[] jars = jarLibDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    classpath.add(jar.getAbsolutePath());
                }
            }
        }
        
        String classpathStr = String.join(File.pathSeparator, classpath);
        
        // 收集所有 .java 文件
        List<Path> javaFiles = new ArrayList<>();
        Files.walk(srcDir)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(javaFiles::add);
        
        if (javaFiles.isEmpty()) {
            throw new RuntimeException("未找到任何 .java 文件");
        }
        
        // 构建 javac 命令
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-encoding");
        command.add("UTF-8");
        command.add("-source");
        command.add("8");
        command.add("-target");
        command.add("8");
        command.add("-d");
        command.add(outputDir.toString());
        command.add("-classpath");
        command.add(classpathStr);
        
        // 添加所有 Java 文件
        for (Path javaFile : javaFiles) {
            command.add(javaFile.toString());
        }
        
        return executeCommand(command, srcDir.getParent());
    }

    /**
     * 编译单个 Java 文件 -> Class（向后兼容）
     */
    private String compileJavaToClass(Path javaFile, Path outputDir) throws IOException, InterruptedException {
        // 构建 classpath
        List<String> classpath = new ArrayList<>();
        classpath.add(androidHome + "/platforms/android-34/android.jar");
        
        // 添加 jar_lib 目录下的所有 jar
        File jarLibDir = new File(jarLibPath);
        if (jarLibDir.exists() && jarLibDir.isDirectory()) {
            File[] jars = jarLibDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    classpath.add(jar.getAbsolutePath());
                }
            }
        }

        String classpathStr = String.join(":", classpath);

        // 执行 javac
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-classpath");
        command.add(classpathStr);
        command.add("-d");
        command.add(outputDir.toString());
        command.add(javaFile.toString());

        return executeCommand(command, javaFile.getParent());
    }

    /**
     * 转换 Class -> Dex
     */
    private String convertClassToDex(Path classesDir, Path outputDex) throws IOException, InterruptedException {
        String d8Path = androidHome + "/build-tools/" + buildToolsVersion + "/d8";

        // 收集所有 .class 文件
        List<String> command = new ArrayList<>();
        command.add(d8Path);
        command.add("--output");
        command.add(outputDex.getParent().toString());

        // 递归添加所有 .class 文件
        List<Path> classFiles = new ArrayList<>();
        Files.walk(classesDir)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(classFiles::add);
        
        if (classFiles.isEmpty()) {
            throw new RuntimeException("未找到任何 .class 文件");
        }
        
        for (Path classFile : classFiles) {
            command.add(classFile.toString());
        }

        // 添加 jar 依赖作为 classpath
        // d8 的 --classpath 需要每个 jar 单独作为一个参数
        File jarLibDir = new File(jarLibPath);
        if (jarLibDir.exists() && jarLibDir.isDirectory()) {
            File[] jars = jarLibDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                for (File jar : jars) {
                    command.add("--classpath");
                    command.add(jar.getAbsolutePath());
                }
            }
        }

        return executeCommand(command, classesDir.getParent());
    }

    /**
     * 执行命令
     */
    private String executeCommand(List<String> command, Path workDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        log.info("执行命令: {}", String.join(" ", command));

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info(line);
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("命令执行超时");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("命令执行失败，退出码: " + exitCode + "\n" + output);
        }

        return output.toString();
    }

    /**
     * 提取包名
     */
    private String extractPackageName(String javaCode) {
        for (String line : javaCode.split("\n")) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring(8, line.indexOf(';')).trim();
            }
        }
        return "";
    }

    /**
     * 提取类名
     */
    private String extractClassName(String javaCode) {
        for (String line : javaCode.split("\n")) {
            line = line.trim();
            if (line.contains("class ")) {
                int classIdx = line.indexOf("class ");
                String afterClass = line.substring(classIdx + 6).trim();
                int spaceIdx = afterClass.indexOf(' ');
                int braceIdx = afterClass.indexOf('{');
                int endIdx = -1;
                if (spaceIdx > 0 && braceIdx > 0) {
                    endIdx = Math.min(spaceIdx, braceIdx);
                } else if (spaceIdx > 0) {
                    endIdx = spaceIdx;
                } else if (braceIdx > 0) {
                    endIdx = braceIdx;
                } else {
                    endIdx = afterClass.length();
                }
                return afterClass.substring(0, endIdx).trim();
            }
        }
        throw new RuntimeException("无法提取类名");
    }

    /**
     * 检查是否有 .class 文件
     */
    private boolean hasClassFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return false;
        }
        try (var stream = Files.walk(dir)) {
            return stream.anyMatch(p -> p.toString().endsWith(".class"));
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("删除文件失败: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("删除目录失败: {}", dir, e);
        }
    }

    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 查询某个用户未下载的编译成功任务
     */
    public List<DexCompileTask> getUndownloadedTasks(Long userId) {
        return dexCompileTaskMapper.findUndownloadedByUserId(userId);
    }
    
    /**
     * 查询某个用户的所有编译任务
     */
    public List<DexCompileTask> getAllTasks(Long userId) {
        return dexCompileTaskMapper.findAllByUserId(userId);
    }
}

