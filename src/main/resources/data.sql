INSERT INTO sys_user (id, username, real_name) VALUES
(1, 'student01', '报修学生'),
(2, 'worker01', '维修人员'),
(3, 'cleaner01', '保洁人员');

INSERT INTO rpt_repair_order
(title, description, repair_type, priority, status, reporter_id, repair_fee, material_fee, total_fee, created_at)
VALUES
('宿舍水龙头漏水', '卫生间水龙头持续滴水，需要维修。', 'PLUMBING', 'NORMAL', 'PENDING', 1, 0, 0, 0, CURRENT_TIMESTAMP);

INSERT INTO rpt_cleaning_plan
(area, cleaner_name, plan_date, status, remark, created_at, updated_at)
VALUES
('一号公寓三楼走廊', '张师傅', CURRENT_DATE, 'PENDING', '每日公共区域保洁', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

