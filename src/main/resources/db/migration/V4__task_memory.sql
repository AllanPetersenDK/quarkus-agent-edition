create table if not exists task_memory (
    dedup_key varchar(512) primary key,
    session_id varchar(128) not null,
    task clob not null,
    memory clob not null,
    task_summary clob,
    approach clob,
    final_answer clob,
    correct boolean,
    error_analysis clob,
    embedding_json clob not null
);
