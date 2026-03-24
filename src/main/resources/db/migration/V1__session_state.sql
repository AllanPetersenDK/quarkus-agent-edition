create table if not exists session_state (
    session_id varchar(128) primary key,
    messages_json clob not null
);
