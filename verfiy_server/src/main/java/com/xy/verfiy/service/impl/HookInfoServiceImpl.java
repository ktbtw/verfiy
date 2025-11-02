package com.xy.verfiy.service.impl;

import com.xy.verfiy.domain.Application;
import com.xy.verfiy.domain.HookInfo;
import com.xy.verfiy.mapper.HookInfoMapper;
import com.xy.verfiy.service.ApplicationService;
import com.xy.verfiy.service.HookInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class HookInfoServiceImpl implements HookInfoService {

    private final HookInfoMapper hookInfoMapper;
    private final ApplicationService applicationService;

    public HookInfoServiceImpl(HookInfoMapper hookInfoMapper, ApplicationService applicationService) {
        this.hookInfoMapper = hookInfoMapper;
        this.applicationService = applicationService;
    }

    @Override
    public List<HookInfo> listByApp(Long appId, String owner) {
        Application app = requireOwnedApp(appId, owner);
        if (app == null) {
            return Collections.emptyList();
        }
        return hookInfoMapper.listByApp(app.getId());
    }

    @Override
    public HookInfo getById(Long id, String owner) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        HookInfo info = hookInfoMapper.findById(id);
        if (info == null) {
            return null;
        }
        requireOwnedApp(info.getAppId(), owner);
        return info;
    }

    @Override
    @Transactional
    public HookInfo saveOrUpdate(HookInfo hookInfo, String owner) {
        if (hookInfo == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        Long appId = hookInfo.getAppId();
        if (appId == null) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        Application app = requireOwnedApp(appId, owner);
        if (app == null) {
            throw new IllegalArgumentException("应用不存在或无权访问");
        }

        String pkg = normalizePackage(hookInfo.getPackageName());
        String version = normalizeVersion(hookInfo.getVersion());

        HookInfo existing = hookInfoMapper.findExact(appId, pkg, version);
        if (existing == null && hookInfo.getId() != null) {
            existing = hookInfoMapper.findByIdAndAppId(hookInfo.getId(), appId);
        }

        boolean enabled = hookInfo.getEnabled() == null
                ? (existing != null ? Boolean.TRUE.equals(existing.getEnabled()) : true)
                : Boolean.TRUE.equals(hookInfo.getEnabled());

        if (existing == null) {
            HookInfo toInsert = new HookInfo();
            toInsert.setAppId(appId);
            toInsert.setPackageName(pkg);
            toInsert.setVersion(version);
            toInsert.setEnabled(enabled);
            toInsert.setData(trimNullable(hookInfo.getData()));
            toInsert.setDexData(trimNullable(hookInfo.getDexData()));
            toInsert.setZipData(trimNullable(hookInfo.getZipData()));
            toInsert.setZipVersion(hookInfo.getZipVersion() != null ? hookInfo.getZipVersion() : 0);
            toInsert.setCreatedBy(owner);
            toInsert.setUpdatedBy(owner);
            hookInfoMapper.insert(toInsert);
            return hookInfoMapper.findExact(appId, pkg, version);
        }

        existing.setPackageName(pkg);
        existing.setVersion(version);
        existing.setEnabled(enabled);
        existing.setData(trimNullable(hookInfo.getData()));
        existing.setDexData(trimNullable(hookInfo.getDexData()));
        existing.setZipData(trimNullable(hookInfo.getZipData()));
        existing.setZipVersion(hookInfo.getZipVersion());
        existing.setUpdatedBy(owner);
        hookInfoMapper.update(existing);
        return hookInfoMapper.findExact(appId, pkg, version);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, boolean enabled, String owner) {
        HookInfo info = getOwnedHook(id, owner);
        if (info == null) {
            return false;
        }
        return hookInfoMapper.updateEnabled(info.getId(), info.getAppId(), enabled, owner) > 0;
    }

    @Override
    @Transactional
    public boolean delete(Long id, String owner) {
        HookInfo info = getOwnedHook(id, owner);
        if (info == null) {
            return false;
        }
        return hookInfoMapper.delete(info.getId(), info.getAppId()) > 0;
    }

    @Override
    public HookInfo findEffective(Long appId, String packageName, String version) {
        if (appId == null || !StringUtils.hasText(packageName)) {
            return null;
        }
        String pkg = packageName.trim();
        String ver = normalizeVersion(version);
        return hookInfoMapper.findEffective(appId, pkg, ver);
    }

    private HookInfo getOwnedHook(Long id, String owner) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        HookInfo info = hookInfoMapper.findById(id);
        if (info == null) {
            return null;
        }
        requireOwnedApp(info.getAppId(), owner);
        return info;
    }

    private Application requireOwnedApp(Long appId, String owner) {
        if (appId == null) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        Application app = applicationService.findById(appId);
        if (app == null) {
            throw new IllegalArgumentException("应用不存在");
        }
        String expectedOwner = owner != null ? owner : "admin";
        if (!Objects.equals(expectedOwner, app.getOwner()) && !"admin".equals(expectedOwner)) {
            throw new IllegalArgumentException("无权访问该应用");
        }
        return app;
    }

    private String normalizePackage(String packageName) {
        if (!StringUtils.hasText(packageName)) {
            throw new IllegalArgumentException("packageName 不能为空");
        }
        return packageName.trim();
    }

    private String normalizeVersion(String version) {
        if (!StringUtils.hasText(version)) {
            return "*";
        }
        return version.trim();
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


