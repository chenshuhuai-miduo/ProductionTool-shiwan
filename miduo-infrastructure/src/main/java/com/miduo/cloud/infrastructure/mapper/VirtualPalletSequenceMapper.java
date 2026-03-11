package com.miduo.cloud.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.VirtualPalletSequencePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 虚拟垛标序号表 Mapper（石湾 2 号机）。
 */
@Mapper
public interface VirtualPalletSequenceMapper extends BaseMapper<VirtualPalletSequencePO> {

    /**
     * 递增指定日期+产线号的 CurrentSeq，若记录不存在则不生效（需先确保有记录）。
     */
    @Update("UPDATE VirtualPalletSequence SET CurrentSeq = CurrentSeq + 1, UpdateTime = NOW() WHERE SeqDate = #{seqDate} AND LineCode = #{lineCode}")
    int incrementCurrentSeq(@Param("seqDate") LocalDate seqDate, @Param("lineCode") String lineCode);
}
