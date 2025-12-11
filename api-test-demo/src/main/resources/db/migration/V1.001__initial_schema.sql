create table example (
    id                  bigserial primary key not null,
    case_id             uuid        not null,
    answer_text         text        not null
);
