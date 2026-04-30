create table hmcts_case (
    id               bigserial primary key not null,
    case_reference   text not null,
    defendant_name   text not null
);
