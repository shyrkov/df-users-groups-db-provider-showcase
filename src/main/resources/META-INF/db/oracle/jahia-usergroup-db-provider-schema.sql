
    drop table showcase_user cascade constraints;

    drop table showcase_user_property cascade constraints;

    drop sequence showcase_user_property_seq;

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

    alter table showcase_user_property 
        add constraint uk_user_property_name unique (name, username);

    alter table showcase_user_property 
        add constraint FK_fhguh36caulh6afghosjyc47w 
        foreign key (username) 
        references showcase_user;

    create sequence showcase_user_property_seq;
