-- 格式说明：每条语句之间必须用注释“--”进行分割
-- 启动时加载初始化

-- 初始化角色
-- @sql init_platform_role
INSERT INTO platform_role (id, creator, create_at, updater, update_at, description, code, name)
VALUES
  (1, 1, NOW(), 1, NOW(), '普通用户，拥有最基本个人信息管理等的权限', 'base_user', '普通用户'),
  (2, 1, NOW(), 1, NOW(), '超级用户（管理层），拥有最基本个人信息管理等的权限，以及查看有应用系统的权限', 'super_user', '超级用户'),
  (3, 1, NOW(), 1, NOW(), '应用管理员，拥有应用运行参数配置基本权限，人员角色分配权限', 'admin', '应用管理员'),
  (4, 1, NOW(), 1, NOW(), '平台技术员，拥有较高平台开发配置权限，维护平台的稳定，添加新功能特性', 'op', '平台技术员'),
  (5, 1, NOW(), 1, NOW(), '超级管理员，拥有最高的权限', 'super_admin', '平台维护员');

-- 初始化管理员账号 super_admin/123456
-- @sql init_platform_user
INSERT INTO platform_user (id, PASSWORD, creator, create_at, updater, update_at, salt, login_name, NAME, description, avatar)
  SELECT
    1,
    '1780a6e404d4d40c7f48cf3a9ad06529c70d7932',
    1,
    NOW(),
    1,
    NOW(),
    '9305eae06a145483',
    'super_admin',
    '超级管理员',
    NULL,
    NULL
  FROM DUAL
  WHERE NOT EXISTS(SELECT *
                   FROM platform_user
                   WHERE login_name = 'super_admin');

-- 初始模块信息、菜单信息
-- 无

-- 初始化字典目录
-- INSERT INTO `md_data_item_catalog`(create_at,creator,code,name,description,update_at,updater)
-- VALUES (NOW(), 1, 'DATA_TYPE', '数据类型', '', NOW(), 1);

-- 初始化字典信息
-- INSERT INTO `md_data_item`
-- (creator,update_at,name,inbuilt,description,type,updater,group_code,name,md_data_item_catalog_id,group_code,create_at)
-- VALUES
--   (1,NOW(), 'string', 1, '', 'string', 1, '数据类型', '字符串(string)', 1, 'DATA_TYPE', NOW()),
--   (1,NOW(), 'number', 1, '', 'string', 1, '数据类型', '数值(number)', 1, 'DATA_TYPE', NOW()),
--   (1,NOW(), 'date', 1, '', 'string', 1, '数据类型', '日期(date)', 1, 'DATA_TYPE', NOW()),
--   (1,NOW(), 'eunm', 1, '', 'string', 1, '数据类型', '枚举(eunm)', 1, 'DATA_TYPE', NOW()),
--   (1,NOW(), 'boolean', 1, '', 'string', 1, '数据类型', '布尔(boolean)', 1, 'DATA_TYPE', NOW());

-- 初始化系统配置
INSERT INTO `platform_common_config` (id, create_at, creator, update_at, updater, name, code, value, seq_no, description)
VALUES (1, NOW(), 1, NOW(), 1, '平台启动应用', 'START_APP', 'workbench', 1, '平台启动时触发的应用');

-- 初始化DEMO实体
INSERT INTO `platform_demo_entity` (id, create_at, creator, update_at, updater, name, code, content, type, description)
VALUES (1, NOW(), 1, NOW(), 1, '示例实体1', 'demoEntity1', 'demoEntityContent1', 'type1', '示例实体描述1');
INSERT INTO `platform_demo_entity` (id, create_at, creator, update_at, updater, name, code, content, type, description)
VALUES (2, NOW(), 1, NOW(), 1, '示例实体2', 'demoEntity2', 'demoEntityContent2', 'type2', '示例实体描述2');
INSERT INTO `platform_demo_entity` (id, create_at, creator, update_at, updater, name, code, content, type, description)
VALUES (3, NOW(), 1, NOW(), 1, '示例实体3', 'demoEntity3', 'demoEntityContent3', 'type3', '示例实体描述3');
INSERT INTO `platform_demo_entity` (id, create_at, creator, update_at, updater, name, code, content, type, description)
VALUES (4, NOW(), 1, NOW(), 1, '示例实体4', 'demoEntity4', 'demoEntityContent4', 'type4', '示例实体描述4');

-- 初始化部门
INSERT INTO `platform_org` (`bu`, `creator`, `code`, `seq_no`, `dept`, `description`, `update_at`, `pid`, `type`, `create_at`, `updater`, `name`, `del_status`, `id`)
VALUES (NULL, '1', NULL, '0', NULL, NULL, '2020-03-23 15:49:10', NULL, NULL, '2020-03-23 15:49:19', '1', '研发部', '0', '1');
INSERT INTO `platform_org_r_user` (`default_org`, `bu`, `creator`, `dept`, `del_status`, `update_at`, `id`, `user_id`, `org_id`, `create_at`, `updater`)
VALUES ('0', NULL, '1', NULL, '0', '2020-03-23 15:48:23', NULL, '1', '1', '2020-03-23 15:48:20', '1');
