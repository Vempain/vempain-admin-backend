CREATE TABLE acl
(
	permission_id    BIGINT(20) NOT NULL,
	acl_id           BIGINT(20) NOT NULL,
	create_privilege boolean    NOT NULL,
	delete_privilege boolean    NOT NULL,
	modify_privilege boolean    NOT NULL,
	read_privilege   boolean    NOT NULL,
	unit_id          BIGINT(20) DEFAULT NULL,
	user_id          BIGINT(20) DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE component
(
	id        BIGINT(20) NOT NULL,
	acl_id    BIGINT(20)                           DEFAULT NULL,
	created   DATETIME   NOT NULL,
	creator   BIGINT(20) NOT NULL,
	locked    boolean    NOT NULL,
	modified  DATETIME                             DEFAULT NULL,
	modifier  BIGINT(20)                           DEFAULT NULL,
	comp_data LONGTEXT COLLATE utf8_unicode_ci     DEFAULT NULL,
	comp_name VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_class
(
	id          BIGINT(20) NOT NULL,
	description VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	shortname   VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_common
(
	id                       BIGINT(20) NOT NULL,
	acl_id                   BIGINT(20)                           DEFAULT NULL,
	locked                   boolean    NOT NULL,
	comment                  TEXT COLLATE utf8_unicode_ci         DEFAULT NULL,
	file_class_id            BIGINT(20)                           DEFAULT NULL,
	mimetype                 VARCHAR(64) COLLATE utf8_unicode_ci  DEFAULT NULL,
	converted_file           TEXT COLLATE utf8_unicode_ci         DEFAULT NULL,
	converted_filesize       BIGINT(20)                           DEFAULT NULL,
	converted_sha1sum        VARCHAR(40) COLLATE utf8_unicode_ci  DEFAULT NULL,
	original_datetime        DATETIME                             DEFAULT NULL,
	original_second_fraction INT(2)                               DEFAULT NULL,
	original_document_id     VARCHAR(128)                         DEFAULT NULL,
	app_file                 TEXT COLLATE utf8_unicode_ci         DEFAULT NULL,
	app_filesize             BIGINT(20)                           DEFAULT NULL,
	app_sha1sum              VARCHAR(40) COLLATE utf8_unicode_ci  DEFAULT NULL,
	site_filename            VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	site_filepath            VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	site_filesize            BIGINT(20)                           DEFAULT NULL,
	site_sha1sum             VARCHAR(40) COLLATE utf8_unicode_ci  DEFAULT NULL,
	metadata                 TEXT COLLATE utf8_unicode_ci         DEFAULT NULL,
	creator                  BIGINT(20) NOT NULL,
	created                  DATETIME   NOT NULL,
	modifier                 BIGINT(20)                           DEFAULT NULL,
	modified                 DATETIME                             DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE gallery
(
	id          BIGINT(20) NOT NULL,
	acl_id      BIGINT(20)                           DEFAULT NULL,
	created     DATETIME   NOT NULL,
	creator     BIGINT(20) NOT NULL,
	locked      boolean    NOT NULL,
	modified    DATETIME                             DEFAULT NULL,
	modifier    BIGINT(20)                           DEFAULT NULL,
	description VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	shortname   VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_audio
(
	id        BIGINT(20) NOT NULL,
	parent_id BIGINT(20) DEFAULT NULL,
	length    BIGINT(20) DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_document
(
	id        BIGINT(20) NOT NULL,
	parent_id BIGINT(20) DEFAULT NULL,
	pages     BIGINT(20) DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_image
(
	id        BIGINT(20) NOT NULL,
	height    BIGINT(20) DEFAULT NULL,
	parent_id BIGINT(20) DEFAULT NULL,
	width     BIGINT(20) DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_video
(
	id        BIGINT(20) NOT NULL,
	parent_id BIGINT(20) DEFAULT NULL,
	height    BIGINT(20) DEFAULT NULL,
	width     BIGINT(20) DEFAULT NULL,
	length    BIGINT(20) DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_subject
(
	file_common_id BIGINT(20) NOT NULL,
	subject_id     BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_thumb
(
	id            BIGINT(20) NOT NULL,
	filename      VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	filepath      VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	site_filename VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	site_filepath VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	filesize      BIGINT(20)                           DEFAULT NULL,
	height        BIGINT(20)                           DEFAULT NULL,
	parent_id     BIGINT(20)                           DEFAULT NULL,
	sha1sum       VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	width         BIGINT(20)                           DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE form
(
	id        BIGINT(20) NOT NULL,
	acl_id    BIGINT(20)                           DEFAULT NULL,
	created   DATETIME   NOT NULL,
	creator   BIGINT(20) NOT NULL,
	locked    boolean    NOT NULL,
	modified  DATETIME                             DEFAULT NULL,
	modifier  BIGINT(20)                           DEFAULT NULL,
	form_name VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	layout_id BIGINT(20)                           DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE form_component
(
	sort_order   BIGINT(20) NOT NULL,
	form_id      BIGINT(20) NOT NULL,
	component_id BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE gallery_file
(
	sort_order     BIGINT(20) NOT NULL,
	file_common_id BIGINT(20) NOT NULL,
	gallery_id     BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE language
(
	language_id int(11) NOT NULL,
	description VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	shortname   VARCHAR(2) COLLATE utf8_unicode_ci   DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE layout
(
	id          BIGINT(20) NOT NULL,
	acl_id      BIGINT(20)                           DEFAULT NULL,
	created     DATETIME   NOT NULL,
	creator     BIGINT(20) NOT NULL,
	locked      boolean    NOT NULL,
	modified    DATETIME                             DEFAULT NULL,
	modifier    BIGINT(20)                           DEFAULT NULL,
	layout_name VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	structure   LONGTEXT COLLATE utf8_unicode_ci     DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE page
(
	id        BIGINT(20)                           NOT NULL,
	acl_id    BIGINT(20)                       DEFAULT NULL,
	created   DATETIME                             NOT NULL,
	creator   BIGINT(20)                           NOT NULL,
	locked    boolean                              NOT NULL,
	modified  DATETIME                         DEFAULT NULL,
	modifier  BIGINT(20)                       DEFAULT NULL,
	body      LONGTEXT COLLATE utf8_unicode_ci     NOT NULL,
	form_id   BIGINT(20)                           NOT NULL,
	header    VARCHAR(512) COLLATE utf8_unicode_ci NOT NULL,
	indexlist boolean                              NOT NULL,
	parent_id BIGINT(20)                       DEFAULT NULL,
	path      VARCHAR(255) COLLATE utf8_unicode_ci NOT NULL,
	secure    boolean                              NOT NULL,
	title     VARCHAR(512) COLLATE utf8_unicode_ci NOT NULL,
	cache     LONGTEXT COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE subject
(
	id         BIGINT(20) NOT NULL,
	subject    VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	subject_de VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	subject_en VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	subject_fi VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	subject_se VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE unit
(
	id          BIGINT(20)                           NOT NULL,
	acl_id      BIGINT(20)                           DEFAULT NULL,
	created     DATETIME                             NOT NULL,
	creator     BIGINT(20)                           NOT NULL,
	locked      boolean                              NOT NULL,
	modified    DATETIME                             DEFAULT NULL,
	modifier    BIGINT(20)                           DEFAULT NULL,
	description VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	name        VARCHAR(255) COLLATE utf8_unicode_ci NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE user
(
	id          BIGINT(20)                                                  NOT NULL,
	acl_id      BIGINT(20)                           DEFAULT NULL,
	created     DATETIME                                                    NOT NULL,
	creator     BIGINT(20)                                                  NOT NULL,
	locked      boolean                                                     NOT NULL,
	modified    DATETIME                             DEFAULT NULL,
	modifier    BIGINT(20)                           DEFAULT NULL,
	birthday    DATETIME                                                    NOT NULL,
	description VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	email       VARCHAR(255) COLLATE utf8_unicode_ci                        NOT NULL,
	login_name  VARCHAR(255) COLLATE utf8_unicode_ci                        NOT NULL,
	name        VARCHAR(255) COLLATE utf8_unicode_ci                        NOT NULL,
	nick        VARCHAR(255) COLLATE utf8_unicode_ci                        NOT NULL,
	password    VARCHAR(255) COLLATE utf8_unicode_ci                        NOT NULL,
	pob         VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	priv_type   ENUM ('PRIVATE', 'GROUP', 'PUBLIC') COLLATE utf8_unicode_ci NOT NULL,
	public      boolean                                                     NOT NULL,
	street      VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE user_unit
(
	user_id BIGINT(20) NOT NULL,
	unit_id BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE page_gallery
(
	page_id    BIGINT(20) NOT NULL,
	gallery_id BIGINT(20),
	sort_order     BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

ALTER TABLE acl
	ADD PRIMARY KEY (permission_id);
ALTER TABLE component
	ADD PRIMARY KEY (id);
ALTER TABLE file_class
	ADD PRIMARY KEY (id);
ALTER TABLE file_common
	ADD PRIMARY KEY (id);
ALTER TABLE gallery
	ADD PRIMARY KEY (id);
ALTER TABLE file_image
	ADD PRIMARY KEY (id);
ALTER TABLE file_thumb
	ADD PRIMARY KEY (id);
ALTER TABLE form
	ADD PRIMARY KEY (id);
ALTER TABLE language
	ADD PRIMARY KEY (language_id);
ALTER TABLE layout
	ADD PRIMARY KEY (id);
ALTER TABLE page
	ADD PRIMARY KEY (id);
ALTER TABLE subject
	ADD PRIMARY KEY (id);
ALTER TABLE unit
	ADD PRIMARY KEY (id);

ALTER TABLE file_subject
	ADD PRIMARY KEY (file_common_id, subject_id),
	ADD KEY subject_idx (subject_id);
ALTER TABLE form_component
	ADD PRIMARY KEY (component_id, form_id, sort_order),
	ADD KEY form_idx (form_id);
ALTER TABLE gallery_file
	ADD PRIMARY KEY (file_common_id, gallery_id, sort_order),
	ADD KEY gallery_idx (gallery_id);
ALTER TABLE user_unit
	ADD PRIMARY KEY (user_id, unit_id),
	ADD KEY unit_idx (unit_id);
ALTER TABLE page_gallery
	ADD PRIMARY KEY (page_id, gallery_id, sort_order),
	ADD KEY page_gallery_idx (page_id);

ALTER TABLE file_common
	ADD INDEX sitefilepath_idx (site_filepath),
	ADD INDEX sitefilename_idx (site_filename),
	ADD INDEX sourcefilename_idx (converted_file),
	ADD INDEX appfilename_idx (app_file);

ALTER TABLE user
	ADD PRIMARY KEY (id),
	ADD UNIQUE KEY email_uk (email),
	ADD UNIQUE KEY login_uk (login_name),
	ADD UNIQUE KEY nick_uk (nick);

ALTER TABLE acl
	MODIFY permission_id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE component
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE file_class
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE file_common
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE gallery
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE file_image
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE file_thumb
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE form
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE layout
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE page
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE subject
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE unit
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE user
	MODIFY id BIGINT(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE file_subject
	ADD CONSTRAINT file_subject_file_common_cstr FOREIGN KEY (file_common_id) REFERENCES file_common (id) ON DELETE CASCADE,
	ADD CONSTRAINT file_subject_subject_cstr FOREIGN KEY (subject_id) REFERENCES subject (id) ON DELETE CASCADE;

ALTER TABLE form_component
	ADD CONSTRAINT form_component_form_cstr FOREIGN KEY (form_id) REFERENCES form (id) ON DELETE CASCADE,
	ADD CONSTRAINT form_component_component_cstr FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE;

ALTER TABLE gallery_file
	ADD CONSTRAINT gallery_file_file_common_cstr FOREIGN KEY (file_common_id) REFERENCES file_common (id) ON DELETE CASCADE,
	ADD CONSTRAINT gallery_file_gallery_cstr FOREIGN KEY (gallery_id) REFERENCES gallery (id) ON DELETE CASCADE;

ALTER TABLE user_unit
	ADD CONSTRAINT user_unit_user_cstr FOREIGN KEY (user_id) REFERENCES user (id),
	ADD CONSTRAINT user_unit_unit_cstr FOREIGN KEY (unit_id) REFERENCES unit (id);

ALTER TABLE page_gallery
	ADD CONSTRAINT page_gallery_page_cstr FOREIGN KEY (page_id) REFERENCES page (id),
	ADD CONSTRAINT page_gallery_gallery_cstr FOREIGN KEY (gallery_id) REFERENCES gallery (id);

ALTER TABLE file_image
	ADD CONSTRAINT file_image_file_common_cstr FOREIGN KEY (parent_id) REFERENCES file_common (id) ON DELETE CASCADE;

ALTER TABLE file_thumb
	ADD CONSTRAINT file_thumb_file_common_cstr FOREIGN KEY (parent_id) REFERENCES file_common (id) ON DELETE CASCADE;

ALTER TABLE file_common
	ADD CONSTRAINT file_common_file_class_cstr FOREIGN KEY (file_class_id) REFERENCES file_class (id);

ALTER TABLE page
	ADD CONSTRAINT page_form_cstr FOREIGN KEY (form_id) REFERENCES form (id) ON DELETE CASCADE;

-- User constraints

ALTER TABLE component
	ADD CONSTRAINT component_creator_user_cstr FOREIGN KEY (creator) REFERENCES user (id),
	ADD CONSTRAINT component_modifier_user_cstr FOREIGN KEY (modifier) REFERENCES user (id);

ALTER TABLE file_common
	ADD CONSTRAINT file_common_creator_user_cstr FOREIGN KEY (creator) REFERENCES user (id),
	ADD CONSTRAINT file_common_modifier_user_cstr FOREIGN KEY (modifier) REFERENCES user (id);

ALTER TABLE form
	ADD CONSTRAINT form_creator_user_cstr FOREIGN KEY (creator) REFERENCES user (id),
	ADD CONSTRAINT form_modifier_user_cstr FOREIGN KEY (modifier) REFERENCES user (id);

ALTER TABLE gallery
	ADD CONSTRAINT gallery_creator_user_cstr FOREIGN KEY (creator) REFERENCES user (id),
	ADD CONSTRAINT gallery_modifier_user_cstr FOREIGN KEY (modifier) REFERENCES user (id);

ALTER TABLE layout
	ADD CONSTRAINT layout_creator_user_cstr FOREIGN KEY (creator) REFERENCES user (id),
	ADD CONSTRAINT layout_modifier_user_cstr FOREIGN KEY (modifier) REFERENCES user (id);

ALTER TABLE page
	ADD CONSTRAINT page_creator_user_cstr FOREIGN KEY (creator) REFERENCES user (id),
	ADD CONSTRAINT page_modifier_user_cstr FOREIGN KEY (modifier) REFERENCES user (id);

ALTER TABLE unit
	ADD CONSTRAINT unit_creator_user_cstr FOREIGN KEY (creator) REFERENCES user (id),
	ADD CONSTRAINT unit_modifier_user_cstr FOREIGN KEY (modifier) REFERENCES user (id);

-- Data
INSERT INTO file_class (id, description, shortname)
VALUES (1, 'Binary file', 'binary');
INSERT INTO file_class (id, description, shortname)
VALUES (2, 'Bitmap image files', 'image');
INSERT INTO file_class (id, description, shortname)
VALUES (3, 'Vector image files', 'vector');
INSERT INTO file_class (id, description, shortname)
VALUES (4, 'Audio files', 'audio');
INSERT INTO file_class (id, description, shortname)
VALUES (5, 'Video files', 'video');
INSERT INTO file_class (id, description, shortname)
VALUES (6, 'Document files', 'document');
INSERT INTO file_class (id, description, shortname)
VALUES (7, '(Un)Compressed archive files', 'archive');
INSERT INTO file_class (id, description, shortname)
VALUES (8, 'Executable files including scripts', 'executable');
INSERT INTO file_class (id, description, shortname)
VALUES (9, 'Interactive files (Flash, Shockwave etc)', 'interactive');
INSERT INTO file_class (id, description, shortname)
VALUES (10, 'Various data files, binary or ascii', 'data');
INSERT INTO file_class (id, description, shortname)
VALUES (11, 'Font files', 'font');
INSERT INTO file_class (id, description, shortname)
VALUES (12, 'Icon files', 'icon');

-- Add Admin account
INSERT INTO user (id, acl_id, birthday, created, creator, modified, modifier, email, locked, login_name, name, nick,
				  password, priv_type, pob, public, street)
VALUES (1, 1, '1900-01-01 00:00:00', NOW(), 1, NULL, NULL, 'admin@nohost.nodomain', false, 'admin',
		'Vempain Administrator', 'Admin', 'Disabled',
		'PRIVATE', '', false, '');

-- Adding default ACLs
INSERT INTO acl (permission_id, acl_id, create_privilege, delete_privilege, modify_privilege, read_privilege, unit_id,
				 user_id)
VALUES (1, 1, true, true, true, true, NULL, 1),
	   (2, 2, true, true, true, true, NULL, 1),
	   (3, 3, true, true, true, true, NULL, 1),
	   (4, 4, true, true, true, true, NULL, 1);

-- Add default units
INSERT INTO unit (id, acl_id, created, creator, locked, modified, modifier, description, name)
VALUES (1, 2, NOW(), 1, false, null, null, 'Admin group', 'Admin'),
	   (2, 3, NOW(), 1, false, null, null, 'Poweruser group', 'Poweruser'),
	   (3, 4, NOW(), 1, false, null, null, 'Editor group', 'Editor');
