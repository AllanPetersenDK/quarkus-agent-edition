create table if not exists session_step_trace (
    session_id varchar(128) primary key,
    steps_json clob not null
);
