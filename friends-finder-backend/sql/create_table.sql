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

create index uk_tag_user_id
    on mai1.tag (user_id)
    comment '上传用户普通索引';

