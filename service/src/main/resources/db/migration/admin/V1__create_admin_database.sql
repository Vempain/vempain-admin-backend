CREATE TABLE user_account
(
	id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id         BIGINT       NOT NULL UNIQUE,
	locked         BOOLEAN      NOT NULL DEFAULT false,
	birthday       TIMESTAMP    NOT NULL,
	description    VARCHAR(255),
	email          VARCHAR(255) NOT NULL UNIQUE,
	login_name     VARCHAR(255) NOT NULL UNIQUE,
	name           VARCHAR(255) NOT NULL,
	nick           VARCHAR(255) NOT NULL,
	password       VARCHAR(255) NOT NULL,
	priv_type      VARCHAR(10)  NOT NULL CHECK (priv_type IN ('PRIVATE', 'GROUP', 'PUBLIC')),
	public_account BOOLEAN      NOT NULL DEFAULT false,
	street         VARCHAR(255)          DEFAULT NULL,
	pob            VARCHAR(255)          DEFAULT NULL,
	status		 VARCHAR(10)  NOT NULL CHECK (status IN ('REGISTERED', 'ACTIVE', 'DISABLED')),
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE acl
(
	id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id           BIGINT  NOT NULL,
	create_privilege BOOLEAN NOT NULL,
	delete_privilege BOOLEAN NOT NULL,
	modify_privilege BOOLEAN NOT NULL,
	read_privilege   BOOLEAN NOT NULL,
	unit_id          BIGINT,
	user_id          BIGINT
);

-- Only either unit_id or user_id is not null
ALTER TABLE acl
	ADD CONSTRAINT acl_unit_user_xor CHECK ((unit_id IS NOT NULL AND user_id IS NULL) OR (unit_id IS NULL AND user_id IS NOT NULL));

-- There can be no two rows with the same acl_id, user_id or unit_id
ALTER TABLE acl
	ADD CONSTRAINT acl_unique_acl_id_user_id_unit_id UNIQUE (acl_id, user_id, unit_id);

CREATE TABLE component
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id    BIGINT       NOT NULL UNIQUE,
	comp_data TEXT,
	comp_name VARCHAR(255) NOT NULL UNIQUE,
	locked    BOOLEAN      NOT NULL DEFAULT false,
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE file_class
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	description VARCHAR(255),
	shortname   VARCHAR(255)
);

CREATE TABLE file_common
(
	id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id                   BIGINT    NOT NULL UNIQUE,
	comment                  TEXT,
	file_class_id            BIGINT    NOT NULL,
	mimetype                 VARCHAR(64),
	converted_file           TEXT      NOT NULL UNIQUE,
	converted_filesize       BIGINT,
	converted_sha1sum        VARCHAR(40),
	original_datetime        TIMESTAMP,
	original_second_fraction INT,
	original_document_id     VARCHAR(128),
	site_filename            VARCHAR(255),
	site_filepath            VARCHAR(255),
	site_filesize            BIGINT,
	site_sha1sum             VARCHAR(40),
	metadata                 TEXT,
	locked                   BOOLEAN   NOT NULL DEFAULT false,
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id),
	FOREIGN KEY (file_class_id) REFERENCES file_class (id) ON DELETE CASCADE
);

CREATE TABLE gallery
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id      BIGINT       NOT NULL UNIQUE,
	description VARCHAR(255),
	shortname   VARCHAR(255) NOT NULL UNIQUE,
	locked      BOOLEAN      NOT NULL DEFAULT false,
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE file_audio
(
	id        BIGINT,
	parent_id BIGINT NOT NULL UNIQUE,
	length    BIGINT,
	FOREIGN KEY (parent_id) REFERENCES file_common (id) ON DELETE CASCADE
);

CREATE TABLE file_document
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	parent_id BIGINT NOT NULL UNIQUE,
	pages     BIGINT,
	FOREIGN KEY (parent_id) REFERENCES file_common (id) ON DELETE CASCADE
);

CREATE TABLE file_image
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	parent_id BIGINT NOT NULL UNIQUE,
	height    BIGINT,
	width     BIGINT,
	FOREIGN KEY (parent_id) REFERENCES file_common (id) ON DELETE CASCADE
);

CREATE TABLE file_video
(
	id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	parent_id BIGINT NOT NULL UNIQUE,
	height    BIGINT,
	width     BIGINT,
	length    BIGINT,
	FOREIGN KEY (parent_id) REFERENCES file_common (id) ON DELETE CASCADE
);

CREATE TABLE subject
(
	id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	subject    VARCHAR(255) NOT NULL UNIQUE,
	subject_de VARCHAR(255),
	subject_en VARCHAR(255),
	subject_fi VARCHAR(255),
	subject_se VARCHAR(255)
);

CREATE TABLE file_subject
(
	file_common_id BIGINT NOT NULL,
	subject_id     BIGINT NOT NULL,
	PRIMARY KEY (file_common_id, subject_id),
	FOREIGN KEY (file_common_id) REFERENCES file_common (id) ON DELETE CASCADE,
	FOREIGN KEY (subject_id) REFERENCES subject (id) ON DELETE CASCADE
);

CREATE TABLE file_thumb
(
	id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	parent_id     BIGINT NOT NULL UNIQUE,
	filename      VARCHAR(255),
	filepath      VARCHAR(255),
	site_filename VARCHAR(255),
	site_filepath VARCHAR(255),
	filesize      BIGINT,
	height        BIGINT,
	sha1sum       VARCHAR(255),
	width         BIGINT,
	FOREIGN KEY (parent_id) REFERENCES file_common (id) ON DELETE CASCADE,
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
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
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
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
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

CREATE TABLE gallery_file
(
	file_common_id BIGINT NOT NULL,
	gallery_id     BIGINT NOT NULL,
	sort_order     BIGINT NOT NULL,
	PRIMARY KEY (file_common_id, gallery_id, sort_order),
	FOREIGN KEY (file_common_id) REFERENCES file_common (id) ON DELETE CASCADE,
	FOREIGN KEY (gallery_id) REFERENCES gallery (id) ON DELETE CASCADE
);

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
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id),
	FOREIGN KEY (form_id) REFERENCES form (id),
	FOREIGN KEY (parent_id) REFERENCES page (id)
);

CREATE TABLE unit
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id      BIGINT       NOT NULL UNIQUE,
	description VARCHAR(255),
	name        VARCHAR(255) NOT NULL UNIQUE,
	locked      BOOLEAN      NOT NULL DEFAULT false,
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE user_unit
(
	user_id BIGINT NOT NULL,
	unit_id BIGINT NOT NULL,
	PRIMARY KEY (user_id, unit_id),
	FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
	FOREIGN KEY (unit_id) REFERENCES unit (id) ON DELETE CASCADE
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

-- Data
INSERT INTO file_class (id, description, shortname)
	OVERRIDING SYSTEM VALUE
VALUES (1, 'Binary file', 'binary'),
	   (2, 'Bitmap image files', 'image'),
	   (3, 'Vector image files', 'vector'),
	   (4, 'Audio files', 'audio'),
	   (5, 'Video files', 'video'),
	   (6, 'Document files', 'document'),
	   (7, '(Un)Compressed archive files', 'archive'),
	   (8, 'Executable files including scripts', 'executable'),
	   (9, 'Interactive files (Flash, Shockwave etc)', 'interactive'),
	   (10, 'Various data files, binary or ascii', 'data'),
	   (11, 'Font files', 'font'),
	   (12, 'Icon files', 'icon');

SELECT setval('file_class_id_seq', (SELECT MAX(id) + 1 FROM file_class));

INSERT INTO user_account (id, acl_id, birthday, created, creator, locked, email, login_name, name, nick, password, priv_type, public_account, street, status)
	OVERRIDING SYSTEM VALUE
VALUES (1, 1, '1900-01-01 00:00:00', NOW(), 1, false, 'admin@nohost.nodomain', 'admin', 'Vempain Administrator', 'Admin', 'Disabled', 'PRIVATE', false, '', 'ACTIVE');

SELECT setval('user_account_id_seq', (SELECT MAX(id) + 1 FROM user_account));

-- Adding default ACLs
INSERT INTO acl (id, acl_id, create_privilege, delete_privilege, modify_privilege, read_privilege, unit_id, user_id)
	OVERRIDING SYSTEM VALUE
VALUES (1, 1, true, true, true, true, NULL, 1),
	   (2, 2, true, true, true, true, NULL, 1),
	   (3, 3, true, true, true, true, NULL, 1),
	   (4, 4, true, true, true, true, NULL, 1);

SELECT setval('acl_id_seq', (SELECT MAX(id) + 1 FROM acl));

-- Add default units
INSERT INTO unit (id, acl_id, created, creator, locked, modified, modifier, description, name)
	OVERRIDING SYSTEM VALUE
VALUES (1, 2, NOW(), 1, false, null, null, 'Admin group', 'Admin'),
	   (2, 3, NOW(), 1, false, null, null, 'Poweruser group', 'Poweruser'),
	   (3, 4, NOW(), 1, false, null, null, 'Editor group', 'Editor');

SELECT setval('unit_id_seq', (SELECT MAX(id) + 1 FROM unit));
