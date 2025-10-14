package com.xy.verfiy.service.impl;

import com.xy.verfiy.domain.InviteCode;
import com.xy.verfiy.mapper.InviteCodeMapper;
import com.xy.verfiy.service.InviteCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class InviteCodeServiceImpl implements InviteCodeService {

    private final InviteCodeMapper inviteCodeMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public InviteCodeServiceImpl(InviteCodeMapper inviteCodeMapper) {
        this.inviteCodeMapper = inviteCodeMapper;
    }

    @Override
    @Transactional
    public String generate(String createdBy) {
        String code = generateCode();
        InviteCode ic = new InviteCode();
        ic.setCode(code);
        ic.setCreatedBy(createdBy);
        ic.setUsed(false);
        ic.setCanInvite(false);  // 默认无邀请权限
        ic.setInviteQuota(0);    // 默认配额为0
        inviteCodeMapper.insert(ic);
        return code;
    }

    @Override
    @Transactional
    public List<String> generateBatch(String createdBy, int count, Boolean canInvite, Integer inviteQuota) {
        List<String> codes = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String code = generateCode();
            InviteCode ic = new InviteCode();
            ic.setCode(code);
            ic.setCreatedBy(createdBy);
            ic.setUsed(false);
            ic.setCanInvite(canInvite != null ? canInvite : false);
            ic.setInviteQuota(inviteQuota != null ? inviteQuota : 0);
            inviteCodeMapper.insert(ic);
            codes.add(code);
        }
        return codes;
    }

    @Override
    @Transactional
    public boolean validateAndUse(String code, String usedBy) {
        InviteCode ic = inviteCodeMapper.findByCode(code);
        if (ic == null || Boolean.TRUE.equals(ic.getUsed())) return false;
        return inviteCodeMapper.markUsed(code, usedBy) > 0;
    }

    @Override
    public List<InviteCode> listAll() {
        return inviteCodeMapper.listAll();
    }

    @Override
    public List<InviteCode> listByCreator(String creator) {
        return inviteCodeMapper.listByCreator(creator);
    }

    @Override
    @Transactional
    public boolean deleteByCode(String code, String requester) {
        InviteCode ic = inviteCodeMapper.findByCode(code);
        if (ic == null) return false;
        if (!requester.equals(ic.getCreatedBy())) return false;
        if (Boolean.TRUE.equals(ic.getUsed())) return false;
        return inviteCodeMapper.deleteByCode(code) > 0;
    }

    @Override
    public int countByCreator(String creator) {
        return inviteCodeMapper.countByCreator(creator);
    }

    private String generateCode() {
        byte[] buf = new byte[9]; // 12 chars base64url without padding roughly
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf).toUpperCase();
    }
}


