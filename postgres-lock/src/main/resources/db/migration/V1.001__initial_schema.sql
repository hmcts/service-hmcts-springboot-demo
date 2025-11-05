create table audit (
    id              bigserial primary key not null,
    payload         text        not null
);