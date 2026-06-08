# 维修保洁模块

本项目是公寓管理系统中的维修保洁模块，包含维修工单和保洁计划两部分，可直接作为实训提交代码仓库。

## 模块内容

- 维修工单表：`rpt_repair_order`
- 保洁计划表：`rpt_cleaning_plan`
- 用户表：`sys_user`，用于维修派单时关联 `assignee_id`

## 维修接口

- `GET /api/repair/page`：维修工单分页查询
- `POST /api/repair/report`：提交维修申报
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
- `POST /api/cleaning/plans`：新增保洁计划
- `PUT /api/cleaning/plans/{id}/start`：开始执行
- `PUT /api/cleaning/plans/{id}/complete`：完成保洁
- `PUT /api/cleaning/plans/{id}/skip`：跳过计划

保洁状态流转：

`PENDING -> IN_PROGRESS -> COMPLETED / SKIPPED`

## 关键规则

- 只有 `PENDING` 状态的维修工单可以派单。
- 派单人员通过 `assignee_id` 关联 `sys_user`。
- 维修完成时自动计算费用：`totalFee = repairFee + materialFee`。

## 运行方式

```powershell
..\apache-maven-3.9.4\bin\mvn.cmd spring-boot:run
```

启动后访问：

```text
http://localhost:8080/api/repair/page
http://localhost:8080/api/cleaning/plans
```

## 测试方式

```powershell
..\apache-maven-3.9.4\bin\mvn.cmd test
```

