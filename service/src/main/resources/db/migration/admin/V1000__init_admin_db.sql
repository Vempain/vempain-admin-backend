CREATE TABLE component
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id    BIGINT       NOT NULL UNIQUE,
	comp_data TEXT,
	comp_name VARCHAR(255) NOT NULL UNIQUE,
	locked    BOOLEAN      NOT NULL DEFAULT false,
	creator   BIGINT       NOT NULL,
	created   TIMESTAMP    NOT NULL,
	modifier  BIGINT,
	modified  TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE gallery
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id      BIGINT       NOT NULL UNIQUE,
	description VARCHAR(255),
	shortname   VARCHAR(255) NOT NULL UNIQUE,
	locked      BOOLEAN      NOT NULL DEFAULT false,
	creator     BIGINT       NOT NULL,
	created     TIMESTAMP    NOT NULL,
	modifier    BIGINT,
	modified    TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE subject
(
	id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	subject VARCHAR(255) NOT NULL UNIQUE,
	subject_de VARCHAR(255) DEFAULT NULL,
	subject_en VARCHAR(255) DEFAULT NULL,
	subject_es VARCHAR(255) DEFAULT NULL,
	subject_fi VARCHAR(255) DEFAULT NULL,
	subject_se VARCHAR(255) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS gps_location
(
    id              BIGINT PRIMARY KEY,
    latitude        NUMERIC(15, 5)   NOT NULL,
    latitude_ref    CHAR(1)          NOT NULL,
    longitude       NUMERIC(15, 5)   NOT NULL,
    longitude_ref   CHAR(1)          NOT NULL,
    altitude        DOUBLE PRECISION NULL,
    direction       DOUBLE PRECISION NULL,
    satellite_count INTEGER          NULL,
    country         VARCHAR(255)     NULL,
    state           VARCHAR(255)     NULL,
    city            VARCHAR(255)     NULL,
    street          VARCHAR(255)     NULL,
    sub_location    VARCHAR(255)     NULL
);

CREATE TABLE site_file
(
	id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id     BIGINT       NOT NULL UNIQUE,
	file_name  VARCHAR(255)  NOT NULL,
	file_path  VARCHAR(2048) NOT NULL,
	mime_type  VARCHAR(255)  NOT NULL,
	size       BIGINT        NOT NULL,
	file_type VARCHAR(255) NOT NULL,
	sha256sum  VARCHAR(255)  NOT NULL,
	comment    TEXT,
	metadata   TEXT,
    original_datetime TIMESTAMPTZ NULL,
    rights_holder     TEXT        NULL,
    rights_terms      TEXT        NULL,
    rights_url        TEXT        NULL,
    creator_name      TEXT        NULL,
    creator_email     TEXT        NULL,
    creator_country   TEXT        NULL,
    creator_url       TEXT        NULL,
    location_id       BIGINT      NULL,
    locked            BOOLEAN     NOT NULL DEFAULT false,
	creator    BIGINT       NOT NULL,
	created    TIMESTAMP    NOT NULL,
	modifier   BIGINT,
	modified   TIMESTAMP,
    FOREIGN KEY (location_id) REFERENCES gps_location (id) ON UPDATE NO ACTION ON DELETE SET NULL,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE file_subject
(
	site_file_id BIGINT NOT NULL,
	subject_id   BIGINT NOT NULL,
	PRIMARY KEY (site_file_id, subject_id),
	FOREIGN KEY (site_file_id) REFERENCES site_file (id) ON DELETE CASCADE,
	FOREIGN KEY (subject_id) REFERENCES subject (id) ON DELETE CASCADE
);

CREATE TABLE file_thumb
(
	id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	parent_id    BIGINT       NOT NULL UNIQUE,
	parent_class VARCHAR(255) NOT NULL,
	filename      VARCHAR(255),
	filepath      VARCHAR(255),
	site_filename VARCHAR(255),
	site_filepath VARCHAR(255),
	filesize      BIGINT,
	height        BIGINT,
	sha1sum       VARCHAR(255),
	width         BIGINT,
	FOREIGN KEY (parent_id) REFERENCES site_file (id) ON DELETE CASCADE,
	UNIQUE (filepath, filename),
	UNIQUE (site_filepath, site_filename)
);

CREATE TABLE layout
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id      BIGINT       NOT NULL UNIQUE,
	layout_name VARCHAR(255) NOT NULL UNIQUE,
	structure   TEXT,
	locked      BOOLEAN      NOT NULL DEFAULT false,
	creator     BIGINT       NOT NULL,
	created     TIMESTAMP    NOT NULL,
	modifier    BIGINT,
	modified    TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE form
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id    BIGINT       NOT NULL UNIQUE,
	form_name VARCHAR(255) NOT NULL UNIQUE,
	layout_id BIGINT,
	locked    BOOLEAN      NOT NULL DEFAULT false,
	creator   BIGINT       NOT NULL,
	created   TIMESTAMP    NOT NULL,
	modifier  BIGINT,
	modified  TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id),
	FOREIGN KEY (layout_id) REFERENCES layout (id)
);

CREATE TABLE form_component
(
	form_id      BIGINT NOT NULL,
	component_id BIGINT NOT NULL,
	sort_order   BIGINT NOT NULL,
	PRIMARY KEY (form_id, component_id, sort_order),
	FOREIGN KEY (form_id) REFERENCES form (id) ON DELETE CASCADE,
	FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_site_file_path_name ON site_file (file_path, file_name);

CREATE TABLE gallery_file
(
	site_file_id BIGINT NOT NULL,
	gallery_id   BIGINT NOT NULL,
	sort_order   BIGINT NOT NULL,
	PRIMARY KEY (site_file_id, gallery_id, sort_order),
	FOREIGN KEY (site_file_id) REFERENCES site_file (id) ON DELETE CASCADE,
	FOREIGN KEY (gallery_id) REFERENCES gallery (id) ON DELETE CASCADE
);

CREATE INDEX idx_gallery_file_site_file_id ON gallery_file (site_file_id);

CREATE TABLE language
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	description VARCHAR(255) NOT NULL UNIQUE,
	shortname   VARCHAR(2)   NOT NULL UNIQUE
);

CREATE TABLE page
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id    BIGINT       NOT NULL UNIQUE,
	body      TEXT         NOT NULL,
	form_id   BIGINT       NOT NULL,
	header    VARCHAR(512) NOT NULL,
	indexlist BOOLEAN      NOT NULL DEFAULT false,
	parent_id BIGINT,
	path      VARCHAR(255) NOT NULL UNIQUE,
	secure    BOOLEAN      NOT NULL DEFAULT true,
	title     VARCHAR(512) NOT NULL,
	cache     TEXT,
	locked    BOOLEAN      NOT NULL DEFAULT false,
	creator   BIGINT       NOT NULL,
	created   TIMESTAMP    NOT NULL,
	modifier  BIGINT,
	modified  TIMESTAMP,
	published TIMESTAMP             DEFAULT NULL,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id),
	FOREIGN KEY (form_id) REFERENCES form (id),
	FOREIGN KEY (parent_id) REFERENCES page (id)
);

CREATE TABLE page_gallery
(
	page_id    BIGINT NOT NULL,
	gallery_id BIGINT,
	sort_order BIGINT NOT NULL,
	PRIMARY KEY (page_id, gallery_id, sort_order),
	FOREIGN KEY (page_id) REFERENCES page (id) ON DELETE CASCADE,
	FOREIGN KEY (gallery_id) REFERENCES gallery (id) ON DELETE CASCADE
);

-- New: publish & scan schedules
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

-- Data

INSERT INTO user_account (id, acl_id, birthday, created, creator, locked, email, login_name, name, nick, password, priv_type, public_account, street, status)
	OVERRIDING SYSTEM VALUE
VALUES (1, 1, '1900-01-01 00:00:00', NOW(), 1, false, 'admin@nohost.nodomain', 'admin', 'Vempain Administrator', 'Admin', 'Disabled', 'PRIVATE', false, '',
		'ACTIVE');

SELECT setval('user_account_id_seq', (SELECT MAX(id) + 1 FROM user_account));

INSERT INTO acl (id, acl_id, create_privilege, delete_privilege, modify_privilege, read_privilege, unit_id, user_id)
	OVERRIDING SYSTEM VALUE
VALUES (1, 1, true, true, true, true, NULL, 1),
	   (2, 2, true, true, true, true, NULL, 1),
	   (3, 3, true, true, true, true, NULL, 1),
	   (4, 4, true, true, true, true, NULL, 1);

SELECT setval('acl_id_seq', (SELECT MAX(id) + 1 FROM acl));

INSERT INTO unit (id, acl_id, created, creator, locked, modified, modifier, description, name)
	OVERRIDING SYSTEM VALUE
VALUES (1, 2, NOW(), 1, false, null, null, 'Admin group', 'Admin'),
	   (2, 3, NOW(), 1, false, null, null, 'Poweruser group', 'Poweruser'),
	   (3, 4, NOW(), 1, false, null, null, 'Editor group', 'Editor');

SELECT setval('unit_id_seq', (SELECT MAX(id) + 1 FROM unit));

