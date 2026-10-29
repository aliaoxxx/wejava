DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    user_id         BIGINT       AUTO_INCREMENT NOT NULL PRIMARY KEY,
    dept_id         BIGINT,
    username        VARCHAR(30)  NOT NULL,
    nickname        VARCHAR(30)  NOT NULL,
    email           VARCHAR(50),
    phone           VARCHAR(11),
    sex             CHAR(1)      DEFAULT '2',
    password        VARCHAR(100),
    status          TINYINT      DEFAULT 0,
    del_flag        TINYINT      DEFAULT 0,
    pwd_update_time TIMESTAMP,
    remark          VARCHAR(500),
    create_by       VARCHAR(64),
    create_time     TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP
);

DROP TABLE IF EXISTS sys_dept;

CREATE TABLE sys_dept (
    dept_id     BIGINT       AUTO_INCREMENT NOT NULL PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    ancestors   VARCHAR(500) NOT NULL DEFAULT '',
    dept_name   VARCHAR(30)  NOT NULL,
    order_num   INT          NOT NULL DEFAULT 0,
    leader      VARCHAR(20),
    phone       VARCHAR(11),
    email       VARCHAR(50),
    status      TINYINT      NOT NULL DEFAULT 0,
    del_flag    TINYINT      NOT NULL DEFAULT 0,
    create_by   VARCHAR(64),
    create_time TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP
);

DROP TABLE IF EXISTS sys_user_role;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);
