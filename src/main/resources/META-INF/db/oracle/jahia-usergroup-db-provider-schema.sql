
    drop table showcase_group cascade constraints;

    drop table showcase_group_member cascade constraints;

    drop table showcase_user cascade constraints;

    drop table showcase_user_property cascade constraints;

    drop sequence showcase_member_seq;

    drop sequence showcase_user_property_seq;

    create table showcase_group (
        groupname varchar2(255 char) not null,
        primary key (groupname)
    );

    create table showcase_group_member (
        id number(19,0) not null,
        name varchar2(255 char) not null,
        groupname varchar2(255 char),
        primary key (id)
    );

    create table showcase_user (
        username varchar2(255 char) not null,
        primary key (username)
    );

    create table showcase_user_property (
        id number(19,0) not null,
        name varchar2(255 char) not null,
        value long,
        username varchar2(255 char),
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
