
    alter table showcase_user_property 
        drop constraint FK_fhguh36caulh6afghosjyc47w;

    drop table showcase_user;

    drop table showcase_user_property;

    create table showcase_user (
        username varchar(255) not null,
        primary key (username)
    );

    create table showcase_user_property (
        id bigint identity not null,
        name varchar(255) not null,
        value varchar(4048),
        username varchar(255),
        primary key (id)
    );

    alter table showcase_user_property 
        add constraint uk_user_property_name unique (name, username);

    alter table showcase_user_property 
        add constraint FK_fhguh36caulh6afghosjyc47w 
        foreign key (username) 
        references showcase_user;
