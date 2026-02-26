# view007 - 后端接口设计文档


开发接口时需要注意看好数据库设计明确哪些字段不能为空，如果字符串前端未传值需要存入空字符串。

**接口设计文档**：
接口一增加任务，先操作ProductionOrder表，OrderStatus字段设为1，ProductSourceType字段设为2，所属生产线路先暂时设为001（标注好为暂定），后操作ProductionOrderDetail表。

接口二根据生产单号修改任务，操作的表为ProductionOrderDetail表

接口三根据生产单号删除任务，操作的表为ProductionOrderDetail表，IsDel置为1

接口四分页获取任务，并支持对生产订单、生产批次、产品名称的模糊查询。

