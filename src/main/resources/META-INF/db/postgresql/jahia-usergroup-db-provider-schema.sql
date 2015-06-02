
    alter table showcase_group_member 
        drop constraint FK_mjq926yihma0q2cd5871akrlk;

    alter table showcase_user_property 
        drop constraint FK_fhguh36caulh6afghosjyc47w;

    drop table if exists showcase_group cascade;

    drop table if exists showcase_group_member cascade;

    drop table if exists showcase_user cascade;

    drop table if exists showcase_user_property cascade;

    drop sequence showcase_member_seq;

    drop sequence showcase_user_property_seq;

    create table showcase_group (
        groupname varchar(255) not null,
        primary key (groupname)
    );

    create table showcase_group_member (
        id int8 not null,
        name varchar(255) not null,
        groupname varchar(255),
        primary key (id)
    );

    create table showcase_user (
        username varchar(255) not null,
        primary key (username)
    );

    create table showcase_user_property (
        id int8 not null,
        name varchar(255) not null,
        value varchar(4048),
        username varchar(255),
        primary key (id)
    );

    alter table showcase_group_member 
        add constraint FK_mjq926yihma0q2cd5871akrlk 
        foreign key (groupname) 
        references showcase_group;

    alter table showcase_user_property 
        add constraint uk_user_property_name unique (name, username);

    alter table showcase_user_property 
        add constraint FK_fhguh36caulh6afghosjyc47w 
        foreign key (username) 
        references showcase_user;

    create sequence showcase_member_seq;

    create sequence showcase_user_property_seq;
