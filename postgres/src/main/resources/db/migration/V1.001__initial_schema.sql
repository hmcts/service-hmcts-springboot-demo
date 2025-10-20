create table answer (
    id                  bigserial primary key not null,
    case_id             uuid        not null,
    answer_text         text        not null
);
