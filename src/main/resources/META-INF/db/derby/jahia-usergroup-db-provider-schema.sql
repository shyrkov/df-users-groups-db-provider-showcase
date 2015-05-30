
    alter table showcase_user_property 
        drop constraint FK_fhguh36caulh6afghosjyc47w;

    drop table showcase_user;

    drop table showcase_user_property;

    create table showcase_user (
        username varchar(255) not null,
        primary key (username)
    );

    create table showcase_user_property (
        id bigint generated by default as identity,
        name varchar(255) not null,
        value varchar(4048),
        username varchar(255),
        primary key (id)
    );

    create unique index uk_user_property_name on showcase_user_property (name, username);

    alter table showcase_user_property 
        add constraint FK_fhguh36caulh6afghosjyc47w 
        foreign key (username) 
        references showcase_user;
