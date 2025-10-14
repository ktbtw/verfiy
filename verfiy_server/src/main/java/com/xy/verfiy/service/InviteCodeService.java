package com.xy.verfiy.service;

import com.xy.verfiy.domain.InviteCode;

import java.util.List;

public interface InviteCodeService {
    String generate(String createdBy);
    List<String> generateBatch(String createdBy, int count, Boolean canInvite, Integer inviteQuota);
    boolean validateAndUse(String code, String usedBy);
    List<InviteCode> listAll();
    List<InviteCode> listByCreator(String creator);
    boolean deleteByCode(String code, String requester);
    int countByCreator(String creator);
}


