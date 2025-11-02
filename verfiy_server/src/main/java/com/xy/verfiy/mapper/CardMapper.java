package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.Card;
import com.xy.verfiy.domain.CardStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CardMapper {
    Card findById(@Param("id") Long id);
    Card findByCode(@Param("code") String code);

    List<Card> pageQuery(@Param("appId") Long appId,
                         @Param("keyword") String keyword,
                         @Param("status") CardStatus status,
                         @Param("offset") int offset,
                         @Param("limit") int limit);

    long count(@Param("appId") Long appId, @Param("keyword") String keyword, @Param("status") CardStatus status);

    int insertBatch(@Param("cards") List<Card> cards);

    int updateStatus(@Param("id") Long id,
                     @Param("status") CardStatus status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    int updateStatusAndActivatedAtIfNull(@Param("id") Long id,
                                         @Param("status") CardStatus status,
                                         @Param("activatedAt") LocalDateTime activatedAt,
                                         @Param("updatedAt") LocalDateTime updatedAt);

    int updateStatusActivatedAndExpireIfNull(@Param("id") Long id,
                                             @Param("status") CardStatus status,
                                             @Param("activatedAt") LocalDateTime activatedAt,
                                             @Param("expireAt") LocalDateTime expireAt,
                                             @Param("updatedAt") LocalDateTime updatedAt);

    int updateDisabled(@Param("id") Long id, @Param("disabled") boolean disabled,
                       @Param("updatedAt") LocalDateTime updatedAt);

    int updateExtra(@Param("id") Long id, @Param("extra") String extra, @Param("returnExtra") Boolean returnExtra);

    int deleteByIdAndAppId(@Param("id") Long id, @Param("appId") Long appId);

    // machine bindings
    Integer existsMachine(@Param("cardId") Long cardId, @Param("machine") String machine);
    int insertMachine(@Param("cardId") Long cardId, @Param("machine") String machine);
    int countMachines(@Param("cardId") Long cardId);
    java.util.List<String> listMachines(@Param("cardId") Long cardId);

    int deleteMachinesByCardId(@Param("cardId") Long cardId);
    int deleteMachine(@Param("cardId") Long cardId, @Param("machine") String machine);

    Integer existsMachineForApp(@Param("appId") Long appId, @Param("machine") String machine);
}


