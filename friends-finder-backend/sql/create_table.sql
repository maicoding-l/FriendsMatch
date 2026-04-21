create table if not exists mai1.user
(
    username      varchar(256)                       null comment '用户名',
    user_account  varchar(256)                       not null,
    avatar_url    varchar(1024)                      null comment '头像 URL',
    gender        tinyint                            null comment '性别',
    user_password varchar(512)                       null comment '密码',
    phone         varchar(128)                       null comment '电话',
    email         varchar(512)                       null comment '邮箱',
    user_status   int      default 0                 not null comment '用户状态',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    id            bigint auto_increment comment '编号'
        primary key,
    is_delete     tinyint  default 0                 not null comment '逻辑删除标志',
    user_role     int      default 0                 not null comment '用户角色 0-普通用户 1-管理员',
    planet_code   varchar(512)                       null comment '星球编号'
);

alter table mai1.user add COLUMN tags varchar(1024) null comment '标签列表';

create table mai1.tag
(
    id          bigint auto_increment comment 'id'
        primary key,
    tag_name    varchar(256)                       not null comment '标签名称',
    user_id     bigint                             null comment '用户 id',
    parent_id   bigint                             null comment '父标签 id',
    is_parent   tinyint                            null comment '0-不是，1-是父标签',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '逻辑删除标志',
    constraint uk_tag_tag_name
        unique (tag_name) comment '标签唯一索引'
)
    comment '标签';

create table mai1.team
(
    id          bigint auto_increment comment 'id'
        primary key,
    team_name    varchar(256)                       not null comment '队伍名称',
    description varchar(512)                                 comment '队伍描述',
    max_num     int        default 1                not null comment '最大人数',
    expire_time datetime   null                              comment '过期时间',
    user_id     bigint                             not null comment '队长',
    status    int           default 0               not null comment '可见状态 0-公开 1-私有 2-加密',
    password  varchar(512) null                   comment '密码',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '逻辑删除标志'
)
    comment '队伍';

create table mai1.user_team
(
    id          bigint auto_increment comment 'id'
        primary key,
    team_id bigint                                  not null comment '队伍id',
    user_id bigint                                  not null comment '用户id',
    join_time datetime   null                              comment '加入时间',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '逻辑删除标志'
)
    comment '用户队伍关系';

CREATE TABLE item
(
                      id            BIGINT PRIMARY KEY AUTO_INCREMENT,
                      item_type     TINYINT NOT NULL COMMENT '类型：1-书籍 2-电影 3-音乐',
                      title         VARCHAR(255) NOT NULL COMMENT '名称',
                      cover_url     VARCHAR(512) COMMENT '封面图片',
                      description   TEXT COMMENT '简介/摘要',

                      creator       VARCHAR(255) COMMENT '作者 / 导演 / 艺术家',
                      publish_year  INT COMMENT '发行/出版年份',

                      tags          VARCHAR(255) COMMENT '标签（逗号分隔）',

                      popularity    INT DEFAULT 0   COMMENT '热度（点赞/收藏等统计）',

                      create_time    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL comment '创建时间',
                      update_time    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
                      is_delete   tinyint  default 0                 not null comment '逻辑删除标志'
)
    comment '物品表';

CREATE TABLE user_item (
                                    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    user_id    BIGINT NOT NULL,
                                    item_id    BIGINT NOT NULL,

                                    action     TINYINT NOT NULL COMMENT '1-喜欢 2-收藏 3-看过/读过/听过',
                                    weight     DOUBLE DEFAULT 1.0 COMMENT '边权重',

                                    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL comment '创建时间',
                                    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
                                    is_delete   tinyint  default 0                 not null comment '逻辑删除标志',
                                    UNIQUE KEY uk_user_item (user_id, item_id)
)

    comment '用户-物品关系';

-- 好友申请表
CREATE TABLE friend_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    from_user_id BIGINT NOT NULL COMMENT '发起申请的用户id',
    to_user_id BIGINT NOT NULL COMMENT '接收申请的用户id',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '状态：0-待处理，1-已同意，2-已拒绝',
    message VARCHAR(512) COMMENT '申请消息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    UNIQUE KEY uk_from_to (from_user_id, to_user_id) COMMENT '防止重复申请'
) COMMENT '好友申请表';

-- 好友关系表
CREATE TABLE friend (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    user_id BIGINT NOT NULL COMMENT '用户id',
    friend_id BIGINT NOT NULL COMMENT '好友id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    UNIQUE KEY uk_user_friend (user_id, friend_id) COMMENT '防止重复好友关系',
    INDEX idx_user_id (user_id) COMMENT '用户id索引'
) COMMENT '好友关系表';

-- 聊天消息表
CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    from_user_id BIGINT NOT NULL COMMENT '发送者用户ID',
    to_user_id BIGINT NOT NULL COMMENT '接收者用户ID',
    content TEXT NOT NULL COMMENT '消息内容',
    type TINYINT DEFAULT 0 NOT NULL COMMENT '消息类型：0-文本，1-图片，2-文件',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '消息状态：0-未读，1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    INDEX idx_from_user (from_user_id) COMMENT '发送者索引',
    INDEX idx_to_user (to_user_id) COMMENT '接收者索引',
    INDEX idx_create_time (create_time) COMMENT '创建时间索引'
) COMMENT '聊天消息表';

-- 小组帖子表
CREATE TABLE team_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子ID',
    team_id BIGINT NOT NULL COMMENT '所属小组ID',
    user_id BIGINT NOT NULL COMMENT '发帖用户ID',
    title VARCHAR(256) NOT NULL COMMENT '帖子标题',
    content TEXT NOT NULL COMMENT '帖子内容',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    INDEX idx_team_id (team_id) COMMENT '小组ID索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_create_time (create_time) COMMENT '创建时间索引'
) COMMENT '小组帖子表';

-- 帖子评论表
CREATE TABLE post_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    post_id BIGINT NOT NULL COMMENT '所属帖子ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID（回复评论时使用）',
    reply_to_user_id BIGINT DEFAULT NULL COMMENT '回复给哪个用户',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    INDEX idx_post_id (post_id) COMMENT '帖子ID索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_parent_id (parent_id) COMMENT '父评论ID索引'
) COMMENT '帖子评论表';

-- 物品短评表
CREATE TABLE item_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '短评ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    item_id BIGINT NOT NULL COMMENT '物品ID',
    content TEXT NOT NULL COMMENT '短评内容',
    rating DOUBLE COMMENT '评分（1.0-5.0）',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    INDEX idx_item_id (item_id) COMMENT '物品ID索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    UNIQUE KEY uk_user_item (user_id, item_id) COMMENT '用户-物品唯一索引'
) COMMENT '物品短评表';

-- 短评点赞表
CREATE TABLE review_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    review_id BIGINT NOT NULL COMMENT '短评ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    UNIQUE KEY uk_user_review (user_id, review_id) COMMENT '用户-短评唯一索引'
) COMMENT '短评点赞表';

create index uk_tag_user_id
    on mai1.tag (user_id)
    comment '上传用户普通索引';


# 创建700个工具人
DELIMITER //
CREATE PROCEDURE BatchInsertUsers()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 700 DO
            INSERT INTO user (id, user_account, username, user_password, create_time, update_time, is_delete, user_role)
            VALUES (i, CONCAT('user_', i), CONCAT('User ', i), 'a64001e2f8d5398be63a9f144131f1db', NOW(), NOW(), 0, 0);
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;

-- 执行存储过程
CALL BatchInsertUsers();
-- 删除存储过程（清理）
DROP PROCEDURE BatchInsertUsers;

