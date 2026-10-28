DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    user_id         BIGINT       NOT NULL PRIMARY KEY,
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
