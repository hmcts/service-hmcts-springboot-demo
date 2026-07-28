create table hmcts_case (
    id               bigserial primary key not null,
    case_reference   text not null,
    secure_text      text not null,
    defendent_json   text
);
