package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAccountMapper {
    UserAccount findByUsername(@Param("username") String username);
    int insert(UserAccount user);
    List<UserAccount> findAll();
    int updateInvitePermission(@Param("username") String username, 
                               @Param("canInvite") Boolean canInvite, 
                               @Param("inviteQuota") Integer inviteQuota);
}


