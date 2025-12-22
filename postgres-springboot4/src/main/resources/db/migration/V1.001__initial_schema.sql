create table answer (
    id                  bigserial primary key not null,
    case_id             uuid        not null,
    answer_text         text        not null,
        event_types             varchar(128)[] not null
);

insert into answer (case_id, answer_text, event_types) values('564e81f9-bd80-433f-b127-4aa003f93c69', 'My answer', '{PCR}');
