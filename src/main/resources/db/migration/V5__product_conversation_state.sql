create table if not exists product_conversation_state (
    conversation_id varchar(128) primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    state_json clob not null
);
