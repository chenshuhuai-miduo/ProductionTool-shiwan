package com.miduo.cloud.application.shiwan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.VirtualPalletSequencePO;
import com.miduo.cloud.infrastructure.mapper.VirtualPalletSequenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 石湾 2 号机虚拟垛标序号服务：按日期+产线号从 VirtualPalletSequence 取下一序号，生成 VirtualSerialNumber。
 */
@Service
public class VirtualPalletSequenceService {

    @Autowired
    private VirtualPalletSequenceMapper virtualPalletSequenceMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    /**
     * 生成下一个虚拟垛标（格式：前缀+yyyyMMdd+3位序号+产线号+2位随机数）。
     * 从 shiwan-m2-settings.json 读取前缀、产线号；序号从 VirtualPalletSequence 表递增获取。
     */
    @Transactional(rollbackFor = Exception.class)
    public String generateNextVirtualSerialNumber() {
        ShiwanM2SettingsDto config = ShiwanM2SettingsFileLoader.load();
        String prefix = "V";
        String lineCode = "A";
        if (config != null && config.getPalletRule() != null) {
            if (config.getPalletRule().getPrefix() != null && !config.getPalletRule().getPrefix().isEmpty()) {
                prefix = config.getPalletRule().getPrefix();
            }
            if (config.getPalletRule().getLineCode() != null && !config.getPalletRule().getLineCode().isEmpty()) {
                lineCode = config.getPalletRule().getLineCode();
            }
        }
        LocalDate today = LocalDate.now();
        int seq = getNextSeq(today, lineCode);
        String dateStr = today.format(DATE_FORMAT);
        String seqStr = String.format("%03d", seq);
        int random = RANDOM.nextInt(100);
        String randomStr = String.format("%02d", random);
        return prefix + dateStr + seqStr + lineCode + randomStr;
    }

    private int getNextSeq(LocalDate seqDate, String lineCode) {
        LambdaQueryWrapper<VirtualPalletSequencePO> q = new LambdaQueryWrapper<>();
        q.eq(VirtualPalletSequencePO::getSeqDate, seqDate).eq(VirtualPalletSequencePO::getLineCode, lineCode);
        VirtualPalletSequencePO existing = virtualPalletSequenceMapper.selectOne(q);
        if (existing == null) {
            VirtualPalletSequencePO po = new VirtualPalletSequencePO();
            po.setSeqDate(seqDate);
            po.setLineCode(lineCode);
            po.setCurrentSeq(1);
            virtualPalletSequenceMapper.insert(po);
            return 1;
        }
        virtualPalletSequenceMapper.incrementCurrentSeq(seqDate, lineCode);
        VirtualPalletSequencePO updated = virtualPalletSequenceMapper.selectOne(q);
        return updated != null ? updated.getCurrentSeq() : (existing.getCurrentSeq() + 1);
    }
}
