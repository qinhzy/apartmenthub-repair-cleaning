INSERT INTO sys_user (id, username, real_name) VALUES
(1, 'student01', '报修学生'),
(2, 'worker01', '维修人员'),
(3, 'cleaner01', '保洁人员');

INSERT INTO rpt_repair_order
(title, description, repair_type, priority, status, reporter_id, repair_fee, material_fee, total_fee, created_at)
VALUES
('宿舍水龙头漏水', '卫生间水龙头持续滴水，需要维修。', 'PLUMBING', 'NORMAL', 'PENDING', 1, 0, 0, 0, CURRENT_TIMESTAMP);

INSERT INTO rpt_repair_order
(title, description, repair_type, priority, status, reporter_id, assignee_id,
 repair_fee, material_fee, total_fee, created_at, assigned_at, completed_at, verified_at)
VALUES
('公共区网络中断', '二号楼公共学习区无法连接网络。', 'NETWORK', 'URGENT', 'PENDING', 1, NULL,
 0, 0, 0, CURRENT_TIMESTAMP, NULL, NULL, NULL),
('空调制冷异常', 'A栋 506 空调出风但不制冷。', 'APPLIANCE', 'NORMAL', 'PROCESSING', 1, 2,
 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL),
('书桌抽屉脱轨', '抽屉已经修复，等待住户验收。', 'FURNITURE', 'LOW', 'WAITING_CHECK', 1, 2,
 60, 20, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('走廊灯具闪烁', '灯管与启动器均已更换。', 'OTHER', 'NORMAL', 'COMPLETED', 1, 2,
 40, 35, 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rpt_cleaning_plan
(area, cleaner_name, plan_date, plan_time, status, remark, created_at, updated_at)
VALUES
('一号公寓三楼走廊', '张师傅', CURRENT_DATE, '09:00:00', 'PENDING', '每日公共区域保洁', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('二号公寓电梯厅', '李师傅', CURRENT_DATE, '10:30:00', 'IN_PROGRESS', '清洁电梯门与地面', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('一号公寓垃圾房', '王师傅', CURRENT_DATE, '13:30:00', 'PENDING', '消毒并清运垃圾', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('三号公寓一楼大厅', '陈师傅', CURRENT_DATE, '15:00:00', 'PENDING', '玻璃与公共区域保洁', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
