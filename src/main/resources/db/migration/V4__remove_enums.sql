ALTER TABLE public."result" ALTER COLUMN "result_type" TYPE text USING "result_type"::text;
ALTER TABLE public."query_dispatch" ALTER COLUMN "broker_type" TYPE text USING "broker_type"::text;
