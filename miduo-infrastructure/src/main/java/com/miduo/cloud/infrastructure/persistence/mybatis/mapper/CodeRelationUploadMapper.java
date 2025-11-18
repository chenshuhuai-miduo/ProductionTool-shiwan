package com.miduo.cloud.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationUploadPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 码关系上传表Mapper接口
 */
@Mapper
public interface CodeRelationUploadMapper extends BaseMapper<CodeRelationUploadPO> {
    
}

