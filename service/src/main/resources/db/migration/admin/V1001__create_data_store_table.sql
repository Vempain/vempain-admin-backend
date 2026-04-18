CREATE TABLE data_store
(
	id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	identifier         VARCHAR(255) NOT NULL UNIQUE,
	type               VARCHAR(100) NOT NULL,
	description        TEXT,
	column_definitions TEXT         NOT NULL,
	create_sql         TEXT         NOT NULL,
	fetch_all_sql      TEXT         NOT NULL,
	fetch_subset_sql   TEXT         NOT NULL,
	generated          TIMESTAMP    NOT NULL,
	csv_data           TEXT         NOT NULL,
	created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
