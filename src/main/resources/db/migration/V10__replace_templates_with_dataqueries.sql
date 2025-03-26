CREATE TABLE dataquery
(
    id            SERIAL PRIMARY KEY,
    created_by    TEXT      NOT NULL,
    label         TEXT      NOT NULL,
    comment       TEXT,
    crtdl         TEXT,
    last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result_size   INTEGER,
    expires_at    TIMESTAMP
);

insert into dataquery (created_by, label, comment, crtdl, last_modified, result_size)
select q.created_by,
       sq.label,
       sq.comment,
       CONCAT('{"version":null,"display":"","cohortDefinition":', qc.query_content, '}'),
       q.created_at,
       sq.result_size
from saved_query sq
         left join query q on sq.query_id = q.id
         left join query_content qc on q.query_content_id = qc.id;

insert into dataquery (created_by, label, comment, crtdl, last_modified)
select q.created_by,
       qt.label,
       qt.comment,
       CONCAT('{"version":null,"display":"","cohortDefinition":', qc.query_content, '}'),
       q.created_at
from query_template qt
         left join query q on qt.query_id = q.id
         left join query_content qc on q.query_content_id = qc.id;

drop table query_template;
drop table saved_query;