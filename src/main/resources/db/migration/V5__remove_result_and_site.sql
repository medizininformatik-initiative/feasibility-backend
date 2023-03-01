-- As of https://github.com/medizininformatik-initiative/feasibility-backend/issues/58 , query results
-- must not be persisted in the database, but only be kept in memory for a configurable amount of
-- time. The volatile results are now kept in de.numcodex.feasibility_gui_backend.query.result.ResultService

DROP TABLE public."result";
DROP TABLE public."site"
