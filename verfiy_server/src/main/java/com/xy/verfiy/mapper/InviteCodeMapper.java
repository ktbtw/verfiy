package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.InviteCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InviteCodeMapper {
    int insert(InviteCode inviteCode);
    InviteCode findByCode(@Param("code") String code);
    int markUsed(@Param("code") String code, @Param("usedBy") String usedBy);
    List<InviteCode> listAll();
    List<InviteCode> listByCreator(@Param("creator") String creator);
    int deleteByCode(@Param("code") String code);
    int countByCreator(@Param("creator") String creator);
}


