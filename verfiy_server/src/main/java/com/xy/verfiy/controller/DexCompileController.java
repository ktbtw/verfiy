package com.xy.verfiy.controller;

import com.xy.verfiy.domain.DexCompileTask;
import com.xy.verfiy.domain.UserAccount;
import com.xy.verfiy.mapper.UserAccountMapper;
import com.xy.verfiy.service.DexCompileService;
import com.xy.verfiy.service.CompileQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/dex-compile")
public class DexCompileController {

    @Autowired
    private DexCompileService dexCompileService;
    
    @Autowired
    private UserAccountMapper userAccountMapper;
    
    @Autowired
    private CompileQuotaService compileQuotaService;

    /**
     * 编译 Java 代码为 Dex（支持多文件）
     */
    @PostMapping("/compile")
    @PreAuthorize("isAuthenticated()")  // 修改为：只需要登录即可
    public ResponseEntity<Map<String, Object>> compile(@RequestBody Map<String, Object> request, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前用户
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 检查是否是管理员
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            // 检查编译配额限制
            if (!compileQuotaService.checkCompileAccess(userId, isAdmin)) {
                int remaining = compileQuotaService.getRemainingCompiles(userId, isAdmin);
                response.put("success", false);
                response.put("message", "编译次数超限！普通用户每小时最多编译 5 次，请稍后再试");
                response.put("remainingAccess", remaining);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }
            
            // 支持新旧两种格式：单文件（javaCode）和多文件（files）
            Map<String, String> files = new HashMap<>();
            
            if (request.containsKey("files")) {
                // 新格式：多文件
                @SuppressWarnings("unchecked")
                Map<String, String> filesMap = (Map<String, String>) request.get("files");
                if (filesMap == null || filesMap.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "文件列表不能为空");
                    return ResponseEntity.badRequest().body(response);
                }
                files.putAll(filesMap);
                log.info("开始编译多文件项目，文件数: {}", files.size());
            } else if (request.containsKey("javaCode")) {
                // 旧格式：单文件（向后兼容）
                String javaCode = (String) request.get("javaCode");
                if (javaCode == null || javaCode.trim().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Java 代码不能为空");
                    return ResponseEntity.badRequest().body(response);
                }
                // 从单文件代码中提取包名和类名，转换为文件格式
                String packageName = extractPackageName(javaCode);
                String className = extractClassName(javaCode);
                String filePath = packageName.replace('.', '/') + "/" + className + ".java";
                files.put(filePath, javaCode);
                log.info("开始编译单文件代码，长度: {}", javaCode.length());
            } else {
                response.put("success", false);
                response.put("message", "请提供 javaCode 或 files 参数");
                return ResponseEntity.badRequest().body(response);
            }
            
            DexCompileTask task = dexCompileService.compileMultipleFilesToDex(files, userId);
            
            response.put("success", task.getSuccess());
            response.put("taskId", task.getTaskId());
            response.put("message", task.getSuccess() ? "编译成功" : "编译失败");
            response.put("compileLog", task.getCompileLog());
            
            if (task.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("编译异常", e);
            response.put("success", false);
            response.put("message", "编译异常: " + e.getMessage());
            response.put("compileLog", getStackTrace(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 提取包名
     */
    private String extractPackageName(String javaCode) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
        java.util.regex.Matcher matcher = pattern.matcher(javaCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "com.example";
    }
    
    /**
     * 提取类名
     */
    private String extractClassName(String javaCode) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(javaCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Main";
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 下载 Dex 文件
     */
    @GetMapping("/download/{taskId}")
    @PreAuthorize("isAuthenticated()")  // 修改为：只需要登录即可
    public ResponseEntity<?> download(@PathVariable String taskId) {
        try {
            log.info("下载 Dex 文件，任务ID: {}", taskId);
            
            // 获取 Dex 文件内容
            byte[] dexBytes = dexCompileService.getDexFileBytes(taskId);
            
            // 标记为已下载并删除文件
            dexCompileService.markAsDownloaded(taskId);
            
            Resource resource = new ByteArrayResource(dexBytes);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"classes.dex\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(dexBytes.length)
                    .body(resource);
                    
        } catch (IllegalStateException e) {
            log.warn("下载失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("下载异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "下载失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 查询编译任务状态
     */
    @GetMapping("/task/{taskId}")
    @PreAuthorize("isAuthenticated()")  // 修改为：只需要登录即可
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DexCompileTask task = dexCompileService.getTask(taskId);
            
            if (task == null) {
                response.put("success", false);
                response.put("message", "任务不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("taskId", task.getTaskId());
            response.put("compileSuccess", task.getSuccess());
            response.put("downloaded", task.getDownloaded());
            response.put("log", task.getCompileLog());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询任务异常", e);
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 查询当前用户未下载的编译任务
     */
    @GetMapping("/undownloaded")
    @PreAuthorize("isAuthenticated()")  // 修改为：只需要登录即可
    public ResponseEntity<Map<String, Object>> getUndownloadedTasks(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前用户
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            List<DexCompileTask> tasks = dexCompileService.getUndownloadedTasks(userId);
            
            response.put("success", true);
            response.put("tasks", tasks);
            response.put("count", tasks.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询未下载任务异常", e);
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 查询当前用户的所有编译任务
     */
    @GetMapping("/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAllTasks(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前用户
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            List<DexCompileTask> tasks = dexCompileService.getAllTasks(userId);
            
            response.put("success", true);
            response.put("tasks", tasks);
            response.put("count", tasks.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询任务列表异常", e);
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除编译任务
     */
    @DeleteMapping("/task/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String taskId, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前用户
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 获取任务信息
            DexCompileTask task = dexCompileService.getTask(taskId);
            if (task == null) {
                response.put("success", false);
                response.put("message", "任务不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // 检查任务是否属于当前用户
            if (!task.getUserId().equals(userId)) {
                response.put("success", false);
                response.put("message", "无权删除此任务");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // 删除任务（包括dex文件）
            dexCompileService.deleteTask(taskId);
            
            response.put("success", true);
            response.put("message", "任务已删除");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("删除任务异常", e);
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 批量删除当前用户的所有编译任务
     */
    @DeleteMapping("/tasks/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteAllTasks(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前用户
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 删除当前用户的所有任务
            int deletedCount = dexCompileService.deleteAllTasksByUserId(userId);
            
            response.put("success", true);
            response.put("message", "已删除 " + deletedCount + " 个任务");
            response.put("deletedCount", deletedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量删除任务异常", e);
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 查询当前用户的编译次数配额
     */
    @GetMapping("/quota")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getQuota(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            int remaining = compileQuotaService.getRemainingCompiles(userId, isAdmin);
            
            response.put("success", true);
            response.put("isAdmin", isAdmin);
            response.put("remaining", remaining);
            response.put("limit", isAdmin ? -1 : 5);  // -1 表示无限制
            response.put("windowHours", 1);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询配额异常", e);
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        
        String username = authentication.getName();
        UserAccount user = userAccountMapper.findByUsername(username);
        
        return user != null ? user.getId() : null;
    }
}

