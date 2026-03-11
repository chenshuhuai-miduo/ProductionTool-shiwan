package com.miduo.cloud.variant.zhimeizhai.application.upload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.upload.RealTimeUploadVO;
import com.miduo.cloud.entity.dto.upload.UploadRequestDTO;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper.CodeRelationMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper.ProductionOrderDetailMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.CodeRelationPO;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.ProductionOrderDetailPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * WMS上传应用服务
 */
@Service
public class WmsUploadApplicationService {
    
    @Autowired
    private ProductionOrderDetailMapper productionOrderDetailMapper;
    
    @Autowired
    private CodeRelationMapper codeRelationMapper;
    
    /**
     * 上传数据到WMS
     *
     * @param request 上传请求
     * @return 上传结果
     */
    public ApiResult<String> uploadToWms(UploadRequestDTO request) {
        try {
            // 模拟调用WMS接口 /synq/resources/asns
            // 暂时模拟返回成功结果
            System.out.println("========== WMS上传 ==========");
            System.out.println("托盘码: " + request.getTransportUnit().getTuId());
            System.out.println("产品ID: " + request.getAsnLine().get(0).getProductId());
            System.out.println("数量: " + request.getAsnLine().get(0).getQuantityExpected());
            System.out.println("批次号: " + request.getAsnLine().get(0).getAttributeValue().get(0).getValue());
            
            // 模拟WMS返回结果
            String mockResponse = "{\"errorCode\": \"0\", \"errorText\": \"success\"}";
            System.out.println("WMS返回: " + mockResponse);
            System.out.println("============================");
            
            // errorCode为"0"代表上传成功
            return ApiResult.success("上传成功", mockResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取单个订单的实时上传数据
     *
     * @param taskId 任务ID（ProductionOrderDetail.Id）
     * @return 实时上传数据
     */
    public ApiResult<RealTimeUploadVO> getSingleOrderUploadDataByTaskId(Integer taskId) {
        try {
            if (taskId == null) {
                return ApiResult.error("任务ID不能为空");
            }
            
            // 根据taskId查询订单信息
            ProductionOrderDetailPO order = productionOrderDetailMapper.selectById(taskId);
            
            if (order == null) {
                return ApiResult.error("任务不存在");
            }
            
            // 统计该订单下BigSerialNumber有值的记录数
            Long boxCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, order.getOrderNo())
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getBigSerialNumber)
                    .ne(CodeRelationPO::getBigSerialNumber, "")
            );
            
            RealTimeUploadVO vo = new RealTimeUploadVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setBoxCount(boxCount.intValue());
            vo.setUploadStatus("上传成功");
            vo.setIsSelected(true); // 新查询的就是当前选中的
            
            return ApiResult.success("查询成功", vo);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取单个订单的实时上传数据（通过订单号）
     * 
     * @param orderNo 订单号
     * @return 实时上传数据
     * @deprecated 当一个订单有多个产品明细时，应使用getSingleOrderUploadDataByTaskId
     */
    @Deprecated
    public ApiResult<RealTimeUploadVO> getSingleOrderUploadData(String orderNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单号不能为空");
            }
            
            // 统计该订单下BigSerialNumber有值的记录数
            Long boxCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getBigSerialNumber)
                    .ne(CodeRelationPO::getBigSerialNumber, "")
            );
            
            RealTimeUploadVO vo = new RealTimeUploadVO();
            vo.setOrderNo(orderNo);
            vo.setBoxCount(boxCount.intValue());
            vo.setUploadStatus("上传成功");
            vo.setIsSelected(true);
            
            return ApiResult.success("查询成功", vo);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
}

