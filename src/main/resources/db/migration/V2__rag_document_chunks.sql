create table if not exists rag_document_chunk (
    chunk_id varchar(128) primary key,
    source_id varchar(128) not null,
    chunk_index integer not null,
    chunk_text clob not null,
    metadata_json clob not null,
    embedding_json clob not null
);
