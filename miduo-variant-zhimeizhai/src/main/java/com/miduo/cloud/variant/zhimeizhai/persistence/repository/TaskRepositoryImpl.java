package com.miduo.cloud.variant.zhimeizhai.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.common.dto.PageInput;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.domain.task.model.Task;
import com.miduo.cloud.domain.task.repository.TaskRepository;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper.ProductionOrderDetailMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper.ProductionOrderMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.ProductionOrderDetailPO;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.ProductionOrderPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务仓储实现（致美斋产线）
 */
@Repository
public class TaskRepositoryImpl implements TaskRepository {

    @Autowired
    private ProductionOrderDetailMapper detailMapper;
    @Autowired
    private ProductionOrderMapper orderMapper;

    @Override
    public String save(Task task) {
        String orderNo = generateOrderNo();
        ProductionOrderPO orderPO = new ProductionOrderPO();
        orderPO.setOrderNo(orderNo);
        orderPO.setType(task.getType());
        orderPO.setOrderStatus(task.getOrderStatus() != null ? task.getOrderStatus() : 0);
        orderPO.setOrderSumCount(0);
        orderPO.setProductSumCount(task.getProductSumCount());
        orderPO.setDmakeDate(task.getDmakeDate());
        orderPO.setCreateTime(LocalDateTime.now());
        orderPO.setSrcId(0);
        orderPO.setProductSourceType(2);
        orderPO.setProductionLine(1);
        orderMapper.insert(orderPO);

        ProductionOrderDetailPO detailPO = convertToPO(task);
        detailPO.setOrderNo(orderNo);
        detailPO.setOrderId(orderPO.getId());
        detailPO.setCreateTime(LocalDateTime.now());
        detailPO.setAddTime(LocalDateTime.now());
        detailPO.setIsDel(0);
        detailMapper.insert(detailPO);
        return orderNo;
    }

    @Override
    public Optional<Task> findById(Integer id) {
        ProductionOrderDetailPO po = detailMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToModel);
    }

    @Override
    public Optional<Task> findByOrderNo(String orderNo) {
        Page<ProductionOrderDetailPO> page = new Page<>(1, 1);
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("OrderNo", orderNo).eq("IsDel", 0).orderByDesc("Id");
        Page<ProductionOrderDetailPO> result = detailMapper.selectPage(page, wrapper);
        if (result.getRecords().isEmpty()) return Optional.empty();
        return Optional.ofNullable(convertToModel(result.getRecords().get(0)));
    }

    @Override
    public PageOutput<Task> findByPage(PageInput pageInput, String orderNo, Integer type) {
        Page<ProductionOrderDetailPO> page = new Page<>(pageInput.getCurrent(), pageInput.getSize());
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("IsDel", 0);
        if (orderNo != null && !orderNo.isEmpty()) wrapper.like("OrderNo", orderNo);
        if (type != null) wrapper.eq("Type", type);
        wrapper.orderByDesc("Id");
        IPage<ProductionOrderDetailPO> pageResult = detailMapper.selectPage(page, wrapper);
        PageOutput<Task> output = new PageOutput<>();
        output.setCurrent(pageResult.getCurrent());
        output.setSize(pageResult.getSize());
        output.setTotal(pageResult.getTotal());
        output.setPages(pageResult.getPages());
        output.setRecords(pageResult.getRecords().stream().map(this::convertToModel).collect(Collectors.toList()));
        return output;
    }

    @Override
    public PageOutput<Task> findByPageWithFilters(PageInput pageInput, String orderNo,
                                                   String syBatchNo, String productName, Integer orderStatus,
                                                   LocalDateTime productTimeStart, LocalDateTime productTimeEnd) {
        Page<ProductionOrderDetailPO> page = new Page<>(pageInput.getCurrent(), pageInput.getSize());
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("IsDel", 0);
        if (orderNo != null && !orderNo.isEmpty()) wrapper.like("OrderNo", orderNo);
        if (syBatchNo != null && !syBatchNo.isEmpty()) wrapper.like("SyBatchNo", syBatchNo);
        if (productName != null && !productName.isEmpty()) wrapper.like("ProductName", productName);
        if (productTimeStart != null) wrapper.ge("ProductTime", productTimeStart);
        if (productTimeEnd != null) wrapper.le("ProductTime", productTimeEnd.toLocalDate().atTime(23, 59, 59));
        wrapper.orderByDesc("Id");
        IPage<ProductionOrderDetailPO> pageResult = detailMapper.selectPage(page, wrapper);
        List<Task> tasks = pageResult.getRecords().stream().map(this::convertToModel).collect(Collectors.toList());
        if (orderStatus != null) {
            tasks = tasks.stream().filter(t -> t.getOrderStatus() != null && t.getOrderStatus().equals(orderStatus)).collect(Collectors.toList());
        }
        PageOutput<Task> output = new PageOutput<>();
        output.setCurrent(pageResult.getCurrent());
        output.setSize(pageResult.getSize());
        output.setTotal(orderStatus != null ? (long) tasks.size() : pageResult.getTotal());
        output.setPages(orderStatus != null ? (long) Math.ceil((double) tasks.size() / pageInput.getSize()) : pageResult.getPages());
        output.setRecords(tasks);
        return output;
    }

    @Override
    public PageOutput<Task> findByPageWithStatuses(PageInput pageInput, String orderNo,
                                                    String syBatchNo, String productName, List<Integer> orderStatuses,
                                                    LocalDateTime productTimeStart, LocalDateTime productTimeEnd) {
        LocalDateTime endDateTime = productTimeEnd != null ? productTimeEnd.toLocalDate().atTime(23, 59, 59) : null;
        IPage<ProductionOrderDetailPO> pageResult = detailMapper.selectOrdersByStatusesPage(
            new Page<>(pageInput.getCurrent(), pageInput.getSize()),
            orderNo != null && !orderNo.isEmpty() ? orderNo : null,
            syBatchNo != null && !syBatchNo.isEmpty() ? syBatchNo : null,
            productName != null && !productName.isEmpty() ? productName : null,
            orderStatuses, productTimeStart, endDateTime
        );
        PageOutput<Task> output = new PageOutput<>();
        output.setCurrent(pageResult.getCurrent());
        output.setSize(pageResult.getSize());
        output.setTotal(pageResult.getTotal());
        output.setPages(pageResult.getPages());
        output.setRecords(pageResult.getRecords().stream().map(this::convertToModel).collect(Collectors.toList()));
        return output;
    }

    @Override
    public boolean update(Task task) {
        if (task.getId() == null) return false;
        return detailMapper.updateById(convertToPO(task)) > 0;
    }

    @Override
    public boolean deleteById(Integer id) {
        UpdateWrapper<ProductionOrderDetailPO> wrapper = new UpdateWrapper<>();
        wrapper.eq("Id", id).set("IsDel", 1);
        return detailMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        ProductionOrderDetailPO detailPO = detailMapper.selectById(id);
        if (detailPO == null || detailPO.getOrderNo() == null) return false;
        UpdateWrapper<ProductionOrderPO> wrapper = new UpdateWrapper<>();
        wrapper.eq("OrderNo", detailPO.getOrderNo()).set("OrderStatus", status);
        return orderMapper.update(null, wrapper) > 0;
    }

    @Override
    public List<Task> findAll() {
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("IsDel", 0).orderByDesc("Id");
        return detailMapper.selectList(wrapper).stream().map(this::convertToModel).collect(Collectors.toList());
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = new java.util.Random().nextInt(9000) + 1000;
        return "PO" + dateStr + randomNum;
    }

    private ProductionOrderDetailPO convertToPO(Task model) {
        if (model == null) return null;
        ProductionOrderDetailPO po = new ProductionOrderDetailPO();
        po.setId(model.getId());
        po.setOrderNo(model.getOrderNo());
        po.setType(model.getType());
        po.setOrderCount(model.getOrderCount());
        po.setProductCount(model.getProductCount());
        po.setProductId(model.getProductId());
        po.setProductName(model.getProductName());
        po.setProductNo(model.getProductNo());
        po.setProductFormatId(model.getProductFormatId());
        po.setProductFormatName(model.getProductFormatName());
        po.setSyBatchNo(model.getSyBatchNo());
        po.setRatio(model.getRatio());
        po.setProductTime(model.getProductTime());
        po.setTwillendTime(model.getTwillendTime());
        po.setIsDel(model.getIsDel());
        return po;
    }

    private Task convertToModel(ProductionOrderDetailPO po) {
        if (po == null) return null;
        Task model = new Task();
        model.setId(po.getId());
        model.setOrderNo(po.getOrderNo());
        model.setType(po.getType());
        model.setProductCount(po.getProductCount());
        model.setProductId(po.getProductId());
        model.setProductName(po.getProductName());
        model.setProductNo(po.getProductNo());
        model.setProductFormatId(po.getProductFormatId());
        model.setProductFormatName(po.getProductFormatName());
        model.setSyBatchNo(po.getSyBatchNo());
        model.setRatio(po.getRatio());
        model.setProductTime(po.getProductTime());
        model.setTwillendTime(po.getTwillendTime());
        model.setCreateTime(po.getCreateTime());
        model.setIsDel(po.getIsDel());
        model.setOrderCount(po.getOrderCount() != null ? po.getOrderCount() : 0);
        model.setDmakeDate(po.getCreateTime());
        if (po.getOrderNo() != null) {
            ProductionOrderPO orderPO = orderMapper.selectOne(new QueryWrapper<ProductionOrderPO>().eq("OrderNo", po.getOrderNo()));
            if (orderPO != null) {
                model.setOrderStatus(orderPO.getOrderStatus());
                model.setOrderSumCount(orderPO.getOrderSumCount());
                model.setProductSumCount(orderPO.getProductSumCount());
                model.setDmakeDate(orderPO.getDmakeDate());
            }
        }
        return model;
    }
}
