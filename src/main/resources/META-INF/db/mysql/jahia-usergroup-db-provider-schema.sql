
    alter table showcase_group_member 
        drop 
        foreign key FK_mjq926yihma0q2cd5871akrlk;

    alter table showcase_user_property 
        drop 
        foreign key FK_fhguh36caulh6afghosjyc47w;

    drop table if exists showcase_group;

    drop table if exists showcase_group_member;

    drop table if exists showcase_user;

    drop table if exists showcase_user_property;

    create table showcase_group (
        groupname varchar(255) not null,
        primary key (groupname)
    ) ENGINE=InnoDB;

    create table showcase_group_member (
        id bigint not null auto_increment,
        name varchar(255) not null,
        groupname varchar(255),
        primary key (id)
    ) ENGINE=InnoDB;

    create table showcase_user (
        username varchar(255) not null,
        primary key (username)
    ) ENGINE=InnoDB;

    create table showcase_user_property (
        id bigint not null auto_increment,
        name varchar(255) not null,
        value varchar(4048),
        username varchar(255),
        primary key (id)
    ) ENGINE=InnoDB;

    alter table showcase_group_member 
        add index FK_mjq926yihma0q2cd5871akrlk (groupname), 
        add constraint FK_mjq926yihma0q2cd5871akrlk 
        foreign key (groupname) 
        references showcase_group (groupname);

    alter table showcase_user_property 
        add constraint uk_user_property_name unique (name, username);

    alter table showcase_user_property 
        add index FK_fhguh36caulh6afghosjyc47w (username), 
        add constraint FK_fhguh36caulh6afghosjyc47w 
        foreign key (username) 
        references showcase_user (username);
