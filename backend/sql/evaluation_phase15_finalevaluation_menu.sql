-- Stage 1.5 FinalEvaluation permissions under the existing result page.
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2023, '最终确认查询', 2007, 5, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'evaluation:final:list', '#', 'codex', NOW(), 'codex', NOW(), ''),
(2024, '最终确认生成', 2007, 6, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'evaluation:final:generate', '#', 'codex', NOW(), 'codex', NOW(), ''),
(2025, '最终确认修改', 2007, 7, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'evaluation:final:edit', '#', 'codex', NOW(), 'codex', NOW(), ''),
(2026, '最终确认提交', 2007, 8, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'evaluation:final:confirm', '#', 'codex', NOW(), 'codex', NOW(), ''),
(2027, '最终确认删除', 2007, 9, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'evaluation:final:remove', '#', 'codex', NOW(), 'codex', NOW(), '');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 2023),
(1, 2024),
(1, 2025),
(1, 2026),
(1, 2027);
