CREATE TABLE publish_schedule
(
	id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	publish_time    TIMESTAMP    NOT NULL,
	publish_status  VARCHAR(255) NOT NULL,
	publish_message TEXT,
	publish_type    VARCHAR(255) NOT NULL,
	publish_id      BIGINT       NOT NULL,
	created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scan_queue_schedule
(
	id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	source_directory      TEXT      NOT NULL,
	destination_directory TEXT      NOT NULL,
	create_gallery        BOOLEAN   NOT NULL,
	gallery_shortname     VARCHAR(255),
	gallery_description   TEXT,
	create_page           BOOLEAN   NOT NULL,
	page_title            VARCHAR(512),
	page_path             VARCHAR(255),
	page_body             TEXT,
	page_form_id          BIGINT,
	created_by            BIGINT    NOT NULL,
	created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
