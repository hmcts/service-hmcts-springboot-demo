create table task (
    id          bigserial primary key not null,
    name        varchar(255) not null,
    status      varchar(50)  not null default 'PENDING',
    created_at  timestamp    not null default now()
);
