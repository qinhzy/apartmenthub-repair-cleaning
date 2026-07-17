# 维修保洁模块

本项目是公寓管理系统中的维修保洁模块，包含维修工单和保洁计划两部分，可直接作为实训提交代码仓库。

项目同时提供响应式运维工作台，覆盖维修工单搜索、筛选、分页、新建、派单、完工、验收，以及保洁计划的新建和状态流转。前端由 Spring Boot 直接托管，不需要额外安装 Node.js 依赖。

## 模块内容

- 维修工单表：`rpt_repair_order`
- 保洁计划表：`rpt_cleaning_plan`
- 用户表：`sys_user`，用于维修派单时关联 `assignee_id`

## 维修接口

- `GET /api/repair/page`：维修工单分页查询，支持 `status`、`type` 和 `query` 筛选
- `POST /api/repair/report`：提交维修申报，成功返回 `201 Created`
- `PUT /api/repair/assign`：维修派单
- `PUT /api/repair/complete`：维修完成
- `PUT /api/repair/verify`：维修验收

维修状态流转：

`PENDING -> PROCESSING -> WAITING_CHECK -> COMPLETED`

维修类型：

`PLUMBING / FURNITURE / APPLIANCE / NETWORK / OTHER`

优先级：

`URGENT / NORMAL / LOW`

## 保洁接口

- `GET /api/cleaning/plans`：查询保洁计划
- `POST /api/cleaning/plans`：新增保洁计划，成功返回 `201 Created`
- `PUT /api/cleaning/plans/{id}/start`：开始执行
- `PUT /api/cleaning/plans/{id}/complete`：完成保洁
- `PUT /api/cleaning/plans/{id}/skip`：跳过计划

保洁状态流转：

`PENDING -> IN_PROGRESS -> COMPLETED / SKIPPED`

保洁计划支持可选的 `planTime`（`HH:mm`），工作台会按时间排列当天任务。

## 工作台接口

- `GET /api/dashboard/summary`：一次返回首页指标、最近维修工单和当天保洁计划，避免页面串行请求。
- 维修工单响应包含 `reporterName` 与 `assigneeName`，界面无需硬编码人员姓名。

## 关键规则

- 只有 `PENDING` 状态的维修工单可以派单。
- 派单人员通过 `assignee_id` 关联 `sys_user`。
- 维修完成时自动计算费用：`totalFee = repairFee + materialFee`。
- 状态流转使用带前置状态条件的原子更新；并发操作不会重复派单或覆盖已完成状态。
- API 输入长度、ID、金额精度与数据库约束保持一致，并返回字段级校验错误。
- 非法枚举、参数校验失败和错误 JSON 使用一致的错误响应结构，不暴露内部异常。
- 数据库约束同步校验维修状态与派单、完成、验收时间，阻止绕过服务层写入无效生命周期。

## 运行方式

```bash
mvn spring-boot:run
```

启动后访问：

```text
http://localhost:8080/
http://localhost:8080/api/repair/page
http://localhost:8080/api/cleaning/plans
```

## 测试方式

```bash
mvn test
```
