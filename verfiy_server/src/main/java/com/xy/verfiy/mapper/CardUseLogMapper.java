package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.CardUseLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardUseLogMapper {
    int insert(CardUseLog log);
    List<CardUseLog> listByCardId(@Param("cardId") Long cardId);
    int deleteByCardId(@Param("cardId") Long cardId);
}


