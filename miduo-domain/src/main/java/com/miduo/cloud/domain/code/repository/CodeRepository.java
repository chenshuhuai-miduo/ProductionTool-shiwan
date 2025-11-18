package com.miduo.cloud.domain.code.repository;

import com.miduo.cloud.domain.code.model.CodeRelation;

import java.util.List;
import java.util.Optional;

/**
 * 码关联仓储接口
 * 定义码数据访问的抽象接口
 */
public interface CodeRepository {
    
    /**
     * 保存码关联记录
     * @param codeRelation 码关联领域对象
     * @return 保存的记录ID
     */
    Integer save(CodeRelation codeRelation);
    
    /**
     * 批量保存码关联记录
     * @param codeRelations 码关联列表
     * @return 成功保存的数量
     */
    int batchSave(List<CodeRelation> codeRelations);
    
    /**
     * 根据订单号和箱码查询
     * @param orderNo 订单号
     * @param smallSerialNumber 箱码
     * @return 码关联对象
     */
    Optional<CodeRelation> findByOrderNoAndBoxCode(String orderNo, String smallSerialNumber);
    
    /**
     * 根据订单号和TagNo查询最新记录
     * @param orderNo 订单号
     * @param tagNo 标签号
     * @param limit 限制数量
     * @return 码关联列表
     */
    List<CodeRelation> findLatestByTagNo(String orderNo, String tagNo, int limit);
    
    /**
     * 查询订单下所有码记录
     * @param orderNo 订单号
     * @return 码关联列表
     */
    List<CodeRelation> findByOrderNo(String orderNo);
    
    /**
     * 更新码关联记录
     * @param codeRelation 码关联对象
     * @return 是否更新成功
     */
    boolean update(CodeRelation codeRelation);
    
    /**
     * 批量更新托盘码
     * @param ids 记录ID列表
     * @param palletCode 托盘码
     * @return 更新的记录数
     */
    int batchUpdatePalletCode(List<Integer> ids, String palletCode);
    
    /**
     * 逻辑删除记录
     * @param id 记录ID
     * @return 是否删除成功
     */
    boolean deleteById(Integer id);
    
    /**
     * 批量逻辑删除
     * @param ids 记录ID列表
     * @return 删除的记录数
     */
    int batchDelete(List<Integer> ids);
    
    /**
     * 统计指定TagNo的码数量
     * @param orderNo 订单号
     * @param tagNo 标签号
     * @return 码数量
     */
    int countByTagNo(String orderNo, String tagNo);
    
    /**
     * 查询指定长度的箱码记录
     * @param orderNo 订单号
     * @param tagNo 标签号
     * @param codeLength 箱码长度
     * @return 码关联列表
     */
    List<CodeRelation> findByCodeLength(String orderNo, String tagNo, int codeLength);
}

