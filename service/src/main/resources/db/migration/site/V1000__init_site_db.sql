CREATE TABLE web_site_page
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	page_id   BIGINT                      NOT NULL,
	acl_id    BIGINT                      DEFAULT NULL,
	body      TEXT,
	header    VARCHAR(512)                NOT NULL,
	indexlist BOOLEAN                     NOT NULL,
	parent_id BIGINT                      DEFAULT NULL,
	path      VARCHAR(255)                NOT NULL,
	secure    BOOLEAN                     NOT NULL,
	title     VARCHAR(512)                NOT NULL,
	creator   VARCHAR(512)                NOT NULL,
	created   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	modifier  VARCHAR(512)                DEFAULT NULL,
	modified  TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
	published TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
	cache     TEXT                        DEFAULT NULL
);

CREATE TABLE web_site_file
(
	id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	file_id  BIGINT       NOT NULL,
	acl_id BIGINT NOT NULL,
	comment  TEXT   DEFAULT NULL,
	path     VARCHAR(512) NOT NULL,
	mimetype VARCHAR(255) NOT NULL,
	width    BIGINT DEFAULT NULL,
	height   BIGINT DEFAULT NULL,
	length   BIGINT DEFAULT NULL,
	pages    BIGINT DEFAULT NULL,
	metadata TEXT   DEFAULT NULL
);

CREATE TABLE web_site_subject
(
	id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	subject    VARCHAR(255) DEFAULT NULL,
	subject_de VARCHAR(255) DEFAULT NULL,
	subject_en VARCHAR(255) DEFAULT NULL,
	subject_es VARCHAR(255) DEFAULT NULL,
	subject_fi VARCHAR(255) DEFAULT NULL,
	subject_se VARCHAR(255) DEFAULT NULL
);

CREATE TABLE web_site_file_subject
(
	file_id    BIGINT NOT NULL,
	subject_id BIGINT NOT NULL,
	CONSTRAINT fk_file_subject_file_id FOREIGN KEY (file_id) REFERENCES web_site_file (id) ON DELETE CASCADE,
	CONSTRAINT fk_file_subject_subject_id FOREIGN KEY (subject_id) REFERENCES web_site_subject (id) ON DELETE CASCADE
);

CREATE TABLE web_site_page_subject
(
	page_id    BIGINT NOT NULL,
	subject_id BIGINT NOT NULL,
	CONSTRAINT fk_page_subject_page_id FOREIGN KEY (page_id) REFERENCES web_site_page (id) ON DELETE CASCADE
);

CREATE TABLE web_site_gallery
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	gallery_id  BIGINT                      NOT NULL,
	shortname   VARCHAR(255)                DEFAULT NULL,
	description VARCHAR(255)                DEFAULT NULL,
	creator     BIGINT                      NOT NULL,
	created     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	modifier    BIGINT                      DEFAULT NULL,
	modified    TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL
);

CREATE TABLE web_site_gallery_file
(
	gallery_id BIGINT NOT NULL,
	file_id    BIGINT NOT NULL,
	sort_order BIGINT NOT NULL,
	CONSTRAINT fk_gallery_file_gallery_id FOREIGN KEY (gallery_id) REFERENCES web_site_gallery (id) ON DELETE CASCADE
);

CREATE TABLE web_site_gallery_subject
(
	gallery_id BIGINT NOT NULL,
	subject_id BIGINT NOT NULL,
	CONSTRAINT fk_gallery_subject_gallery_id FOREIGN KEY (gallery_id) REFERENCES web_site_gallery (id) ON DELETE CASCADE
);
