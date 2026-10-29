-- 关联表补外键约束
--
-- 之前 20 张表一个外键都没有，关联行完全靠应用层保证。
-- 实测生产库无孤儿数据，这里补上约束，并用 ON DELETE CASCADE 让"删除父行时
-- 关联行自动清理"由数据库兜底（例如删岗位时 sys_user_post 不会留下悬空记录）。
--
-- 注意：用户/角色/部门是逻辑删除（del_flag），物理删除才触发级联。

ALTER TABLE sys_user_role
    ADD CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id) ON DELETE CASCADE;

ALTER TABLE sys_role_menu
    ADD CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (menu_id) ON DELETE CASCADE;

ALTER TABLE sys_role_dept
    ADD CONSTRAINT fk_role_dept_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_role_dept_dept FOREIGN KEY (dept_id) REFERENCES sys_dept (dept_id) ON DELETE CASCADE;

ALTER TABLE sys_user_post
    ADD CONSTRAINT fk_user_post_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_post_post FOREIGN KEY (post_id) REFERENCES sys_post (post_id) ON DELETE CASCADE;
