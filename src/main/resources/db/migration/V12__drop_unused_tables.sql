DROP INDEX idx_mapping_name_mapping;
ALTER TABLE contextualized_termcode DROP CONSTRAINT mapping_id_fk;
ALTER TABLE contextualized_termcode DROP COLUMN mapping_id;

DROP TABLE mapping;
DROP TABLE contextualized_termcode_to_criteria_set;
DROP TABLE criteria_set;