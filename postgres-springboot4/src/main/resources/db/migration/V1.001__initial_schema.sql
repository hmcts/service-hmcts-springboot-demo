create table answer (
    id                  bigserial primary key not null,
    case_id             uuid        not null,
    answer_text         text        not null,
    hearing_date        date,
    timestamp           timestamp not null
);

insert into answer (case_id, answer_text, hearing_date, timestamp) values('564e81f9-bd80-433f-b127-4aa003f93c69', 'My answer', '2000-01-01', now());
