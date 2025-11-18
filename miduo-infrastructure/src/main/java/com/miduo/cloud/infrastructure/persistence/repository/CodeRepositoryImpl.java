package com.miduo.cloud.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.domain.code.model.CodeRelation;
import com.miduo.cloud.domain.code.repository.CodeRepository;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.CodeRelationMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 码关联仓储实现类
 * 实现领域层定义的CodeRepository接口
 */
@Repository
public class CodeRepositoryImpl implements CodeRepository {
    
    @Autowired
    private CodeRelationMapper codeRelationMapper;
    
    @Override
    public Integer save(CodeRelation codeRelation) {
        CodeRelationPO po = convertToPO(codeRelation);
        codeRelationMapper.insert(po);
        return po.getId();
    }
    
    @Override
    public int batchSave(List<CodeRelation> codeRelations) {
        if (codeRelations == null || codeRelations.isEmpty()) {
            return 0;
        }
        List<CodeRelationPO> poList = codeRelations.stream()
                .map(this::convertToPO)
                .collect(Collectors.toList());
        return codeRelationMapper.insertBatch(poList);
    }
    
    @Override
    public Optional<CodeRelation> findByOrderNoAndBoxCode(String orderNo, String smallSerialNumber) {
        Page<CodeRelationPO> page = new Page<>(1, 1);
        QueryWrapper<CodeRelationPO> wrapper = new QueryWrapper<>();
        wrapper.eq("OrderNo", orderNo)
               .eq("SmallSerialNumber", smallSerialNumber)
               .eq("IsDel", 0)
               .orderByDesc("ID");
        
        Page<CodeRelationPO> result = codeRelationMapper.selectPage(page, wrapper);
        CodeRelationPO po = result.getRecords().isEmpty() ? null : result.getRecords().get(0);
        return Optional.ofNullable(po).map(this::convertToModel);
    }
    
    @Override
    public List<CodeRelation> findLatestByTagNo(String orderNo, String tagNo, int limit) {
        Page<CodeRelationPO> page = new Page<>(1, limit);
        QueryWrapper<CodeRelationPO> wrapper = new QueryWrapper<>();
        wrapper.eq("OrderNo", orderNo)
               .eq("TagNo", tagNo)
               .eq("IsDel", 0)
               .orderByDesc("ID");
        
        Page<CodeRelationPO> result = codeRelationMapper.selectPage(page, wrapper);
        List<CodeRelationPO> poList = result.getRecords();
        return poList.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CodeRelation> findByOrderNo(String orderNo) {
        QueryWrapper<CodeRelationPO> wrapper = new QueryWrapper<>();
        wrapper.eq("OrderNo", orderNo)
               .eq("IsDel", 0)
               .orderByDesc("ID");
        
        List<CodeRelationPO> poList = codeRelationMapper.selectList(wrapper);
        return poList.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean update(CodeRelation codeRelation) {
        if (codeRelation.getId() == null) {
            return false;
        }
        CodeRelationPO po = convertToPO(codeRelation);
        return codeRelationMapper.updateById(po) > 0;
    }
    
    @Override
    public int batchUpdatePalletCode(List<Integer> ids, String palletCode) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return codeRelationMapper.batchUpdatePalletCode(ids, palletCode);
    }
    
    @Override
    public boolean deleteById(Integer id) {
        UpdateWrapper<CodeRelationPO> wrapper = new UpdateWrapper<>();
        wrapper.eq("ID", id).set("IsDel", 1);
        return codeRelationMapper.update(null, wrapper) > 0;
    }
    
    @Override
    public int batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        UpdateWrapper<CodeRelationPO> wrapper = new UpdateWrapper<>();
        wrapper.in("ID", ids).set("IsDel", 1);
        return codeRelationMapper.update(null, wrapper);
    }
    
    @Override
    public int countByTagNo(String orderNo, String tagNo) {
        QueryWrapper<CodeRelationPO> wrapper = new QueryWrapper<>();
        wrapper.eq("OrderNo", orderNo)
               .eq("TagNo", tagNo)
               .eq("IsDel", 0);
        return codeRelationMapper.selectCount(wrapper).intValue();
    }
    
    @Override
    public List<CodeRelation> findByCodeLength(String orderNo, String tagNo, int codeLength) {
        QueryWrapper<CodeRelationPO> wrapper = new QueryWrapper<>();
        wrapper.eq("OrderNo", orderNo)
               .eq("TagNo", tagNo)
               .eq("IsDel", 0)
               .apply("LEN(SmallSerialNumber) = {0}", codeLength)
               .orderByDesc("ID");
        
        List<CodeRelationPO> poList = codeRelationMapper.selectList(wrapper);
        return poList.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }
    
    /**
     * 将领域模型转换为持久化对象
     */
    private CodeRelationPO convertToPO(CodeRelation model) {
        if (model == null) {
            return null;
        }
        CodeRelationPO po = new CodeRelationPO();
        BeanUtils.copyProperties(model, po);
        return po;
    }
    
    /**
     * 将持久化对象转换为领域模型
     */
    private CodeRelation convertToModel(CodeRelationPO po) {
        if (po == null) {
            return null;
        }
        CodeRelation model = new CodeRelation();
        BeanUtils.copyProperties(po, model);
        return model;
    }
}

