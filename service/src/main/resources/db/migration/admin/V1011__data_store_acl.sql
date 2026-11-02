-- Data sets become ACL-bearing resources like pages, galleries and site files: the data_store table gets the columns of
-- AbstractVempainEntity (acl_id, locked, creator, created, modifier, modified). created_at/updated_at are carried over into
-- created/modified. Every existing data set gets an ACL of its own granting all privileges to the administrator (user 1),
-- the account the file backend's API tokens act as.

ALTER TABLE data_store
	ADD COLUMN acl_id   BIGINT,
	ADD COLUMN locked   BOOLEAN NOT NULL DEFAULT FALSE,
	ADD COLUMN creator  BIGINT,
	ADD COLUMN created  TIMESTAMP,
	ADD COLUMN modifier BIGINT,
	ADD COLUMN modified TIMESTAMP;

UPDATE data_store
SET created  = COALESCE(created_at, CURRENT_TIMESTAMP),
	creator  = 1,
	modified = CASE WHEN updated_at IS DISTINCT FROM created_at THEN updated_at END,
	modifier = CASE WHEN updated_at IS DISTINCT FROM created_at THEN 1 END;

-- One new ACL per data set, numbered after the highest ACL in use
UPDATE data_store
SET acl_id = numbered.acl_id
FROM (SELECT id, (SELECT COALESCE(MAX(acl_id), 0) FROM acl) + ROW_NUMBER() OVER (ORDER BY id) AS acl_id
	  FROM data_store) AS numbered
WHERE data_store.id = numbered.id;

INSERT INTO acl (acl_id, create_privilege, delete_privilege, modify_privilege, read_privilege, unit_id, user_id)
SELECT acl_id, TRUE, TRUE, TRUE, TRUE, NULL, 1
FROM data_store;

-- Keep the ACL id sequence ahead of the ids handed out above
SELECT setval('acl_acl_id_seq', (SELECT COALESCE(MAX(acl_id), 0) FROM acl) + 1, false);

ALTER TABLE data_store
	ALTER COLUMN acl_id SET NOT NULL,
	ALTER COLUMN creator SET NOT NULL,
	ALTER COLUMN created SET NOT NULL,
	ADD CONSTRAINT uq_data_store_acl_id UNIQUE (acl_id),
	DROP COLUMN created_at,
	DROP COLUMN updated_at;
