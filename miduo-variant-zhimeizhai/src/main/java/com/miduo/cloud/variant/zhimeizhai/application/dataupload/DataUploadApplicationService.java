package com.miduo.cloud.variant.zhimeizhai.application.dataupload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.dataupload.DataUploadOrderVO;
import com.miduo.cloud.entity.dto.dataupload.DataUploadTaskVO;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper.CodeRelationMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper.ProductionOrderDetailMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper.ProductionOrderMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.CodeRelationPO;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.ProductionOrderDetailPO;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.ProductionOrderPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据上传应用服务
 */
@Service
public class DataUploadApplicationService {
    
    @Autowired
    private ProductionOrderDetailMapper productionOrderDetailMapper;
    
    @Autowired
    private ProductionOrderMapper productionOrderMapper;
    
    @Autowired
    private CodeRelationMapper codeRelationMapper;
    
    /**
     * 查询所有生产订单
     */
    public ApiResult<List<DataUploadOrderVO>> queryProductionOrders() {
        try {
            List<ProductionOrderDetailPO> orderList = productionOrderDetailMapper.selectList(
                new LambdaQueryWrapper<ProductionOrderDetailPO>()
                    .eq(ProductionOrderDetailPO::getIsDel, 0)
                    .orderByDesc(ProductionOrderDetailPO::getCreateTime)
            );
            
            List<DataUploadOrderVO> voList = orderList.stream().map(po -> {
                DataUploadOrderVO vo = new DataUploadOrderVO();
                vo.setOrderNo(po.getOrderNo());
                vo.setProductNo(po.getProductNo());  // 设置产品编号
                vo.setProductName(po.getProductName());
                vo.setProductCount(po.getProductCount());
                vo.setProductTime(po.getProductTime());
                vo.setType(po.getType());
                
                // 查询完成数量：使用ProductionOrderDetail表的OrderCount字段（与任务管理列表保持一致）
                vo.setCompletedCount(po.getOrderCount() != null ? po.getOrderCount() : 0);
                
                // 查询生产状态：从ProductionOrder表获取OrderStatus
                ProductionOrderPO orderPO = productionOrderMapper.selectOne(
                    new LambdaQueryWrapper<ProductionOrderPO>()
                        .eq(ProductionOrderPO::getOrderNo, po.getOrderNo())
                );
                if (orderPO != null) {
                    vo.setProductionStatus(orderPO.getOrderStatus());
                }
                
                // 上传状态：默认为"上传完成"
                vo.setUploadStatus("上传完成");
                
                return vo;
            }).collect(Collectors.toList());
            
            return ApiResult.success("查询成功", voList);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据订单编号查询生产任务数据
     */
    public ApiResult<List<DataUploadTaskVO>> queryProductionTasks(String orderNo) {
        try {
            if (orderNo == null || orderNo.isEmpty()) {
                return ApiResult.error("订单编号不能为空");
            }
            
            List<CodeRelationPO> taskList = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .orderByDesc(CodeRelationPO::getAddTime)
            );
            
            List<DataUploadTaskVO> voList = taskList.stream().map(po -> {
                DataUploadTaskVO vo = new DataUploadTaskVO();
                vo.setLayer1(po.getSmallSerialNumber());
                vo.setAddTime(po.getAddTime());
                vo.setLayer2(po.getMediumSerialNumber());
                vo.setLayer3(po.getBigSerialNumber());
                vo.setLayer4(po.getBiggerSerialNumber());
                return vo;
            }).collect(Collectors.toList());
            
            return ApiResult.success("查询成功", voList);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询生产订单（数据上传页面专用，只查询已完成状态的订单）
     * 使用关联查询直接在SQL层面过滤已完成的订单，保证每页返回正确的数据条数
     */
    public ApiResult<PageOutput<DataUploadOrderVO>> queryProductionOrdersByPage(Integer pageNum, Integer pageSize, String orderNo, String productName) {
        try {
            // 使用自定义Mapper方法，直接在SQL层面关联查询并过滤已完成状态的订单
            Page<ProductionOrderDetailPO> page = new Page<>(pageNum, pageSize);
            IPage<ProductionOrderDetailPO> resultPage = productionOrderDetailMapper.selectCompletedOrdersPage(
                page,
                orderNo != null ? orderNo.trim() : null,
                productName != null ? productName.trim() : null
            );
            
            // 转换为VO
            List<DataUploadOrderVO> voList = resultPage.getRecords().stream()
                .map(po -> {
                    DataUploadOrderVO vo = new DataUploadOrderVO();
                    vo.setOrderNo(po.getOrderNo());
                    vo.setProductNo(po.getProductNo());  // 设置产品编号
                    vo.setProductName(po.getProductName());
                    vo.setProductCount(po.getProductCount());
                    vo.setProductTime(po.getProductTime());
                    vo.setType(po.getType());
                    
                    // 查询完成数量：使用ProductionOrderDetail表的OrderCount字段（与任务管理列表保持一致）
                    vo.setCompletedCount(po.getOrderCount() != null ? po.getOrderCount() : 0);
                    
                    // 查询生产状态（由于SQL已过滤，这里直接设置为已完成）
                    vo.setProductionStatus(2);
                    
                    // 上传状态
                    vo.setUploadStatus("上传完成");
                    
                    return vo;
                })
                .collect(Collectors.toList());
            
            // 构建分页输出
            PageOutput<DataUploadOrderVO> pageOutput = new PageOutput<>(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                voList
            );
            
            return ApiResult.success("查询成功", pageOutput);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询生产任务数据（支持按ProductNO和VirtualSerialNumber筛选）
     * @param virtualSerialNumber 虚拟序列号（可选，用于搜索码后只显示相同VirtualSerialNumber的数据）
     */
    public ApiResult<PageOutput<DataUploadTaskVO>> queryProductionTasksByPage(String orderNo, String productNo, String virtualSerialNumber, Integer pageNum, Integer pageSize) {
        try {
            if (orderNo == null || orderNo.isEmpty()) {
                return ApiResult.error("订单编号不能为空");
            }
            
            // 构建查询条件
            LambdaQueryWrapper<CodeRelationPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CodeRelationPO::getOrderNo, orderNo)
                       .eq(CodeRelationPO::getIsDel, 0);
            
            // 添加ProductNO筛选条件（用于区分同一订单下的不同产品）
            if (productNo != null && !productNo.trim().isEmpty()) {
                queryWrapper.eq(CodeRelationPO::getProductNo, productNo.trim());
            }
            
            // 添加VirtualSerialNumber筛选条件（用于搜索码后只显示相同VirtualSerialNumber的数据）
            if (virtualSerialNumber != null && !virtualSerialNumber.trim().isEmpty()) {
                queryWrapper.eq(CodeRelationPO::getVirtualSerialNumber, virtualSerialNumber.trim());
            }
            
            queryWrapper.orderByDesc(CodeRelationPO::getAddTime);
            
            // 分页查询
            Page<CodeRelationPO> page = new Page<>(pageNum, pageSize);
            Page<CodeRelationPO> resultPage = codeRelationMapper.selectPage(page, queryWrapper);
            
            // 转换为VO
            List<DataUploadTaskVO> voList = resultPage.getRecords().stream().map(po -> {
                DataUploadTaskVO vo = new DataUploadTaskVO();
                vo.setLayer1(po.getSmallSerialNumber());
                vo.setAddTime(po.getAddTime());
                vo.setLayer2(po.getMediumSerialNumber());
                vo.setLayer3(po.getBigSerialNumber());
                vo.setLayer4(po.getBiggerSerialNumber());
                return vo;
            }).collect(Collectors.toList());
            
            // 构建分页输出
            PageOutput<DataUploadTaskVO> pageOutput = new PageOutput<>(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                voList
            );
            
            return ApiResult.success("查询成功", pageOutput);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
}
