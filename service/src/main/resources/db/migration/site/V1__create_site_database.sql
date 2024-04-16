CREATE TABLE page
(
	id        BIGINT(20)                           NOT NULL,
	acl_id    BIGINT(20)                           DEFAULT NULL,
	body      LONGTEXT COLLATE utf8_unicode_ci     DEFAULT NULL,
	header    VARCHAR(512) COLLATE utf8_unicode_ci NOT NULL,
	indexlist boolean                              NOT NULL,
	parent_id BIGINT(20)                           DEFAULT NULL,
	path      VARCHAR(255) COLLATE utf8_unicode_ci NOT NULL,
	secure    boolean                              NOT NULL,
	title     VARCHAR(512) COLLATE utf8_unicode_ci NOT NULL,
	creator   VARCHAR(512) COLLATE utf8_unicode_ci NOT NULL,
	created   DATETIME                             NOT NULL,
	modifier  VARCHAR(512) COLLATE utf8_unicode_ci DEFAULT NULL,
	modified  DATETIME                             DEFAULT NULL,
	published DATETIME                             DEFAULT NULL,
	cache     LONGTEXT COLLATE utf8_unicode_ci     DEFAULT NULL,
	UNIQUE KEY unique_page_id (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file
(
	id       BIGINT(20)                           NOT NULL,
	comment  TEXT COLLATE utf8_unicode_ci DEFAULT NULL,
	path     VARCHAR(512) COLLATE utf8_unicode_ci NOT NULL,
	mimetype VARCHAR(255) COLLATE utf8_unicode_ci NOT NULL,
	width    INT(11)                      DEFAULT NULL,
	height   INT(11)                      DEFAULT NULL,
	length   INT(11)                      DEFAULT NULL,
	pages    INT(11)                      DEFAULT NULL,
	metadata TEXT COLLATE utf8_unicode_ci DEFAULT NULL,
	UNIQUE KEY unique_file_id (id)
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
	subject_se VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	UNIQUE KEY unique_subject_id (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE file_subject
(
	file_id    BIGINT(20) NOT NULL,
	subject_id BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

ALTER TABLE file_subject
	ADD CONSTRAINT file_subject_file_cstr FOREIGN KEY (file_id) REFERENCES file (id) ON DELETE CASCADE;

CREATE TABLE page_subject
(
	page_id    BIGINT(20) NOT NULL,
	subject_id BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

ALTER TABLE page_subject
	ADD CONSTRAINT page_subject_page_cstr FOREIGN KEY (page_id) REFERENCES page (id) ON DELETE CASCADE;

CREATE TABLE gallery
(
	id          BIGINT(20) NOT NULL,
	shortname   VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	description VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
	creator     BIGINT(20) NOT NULL,
	created     DATETIME   NOT NULL,
	modifier    BIGINT(20)                           DEFAULT NULL,
	modified    DATETIME                             DEFAULT NULL,
	UNIQUE KEY unique_gallery_id (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

CREATE TABLE gallery_file
(
	gallery_id BIGINT(20) NOT NULL,
	file_id    BIGINT(20) NOT NULL,
	sort_order BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

ALTER TABLE gallery_file
	ADD CONSTRAINT gallery_file_gallery_cstr FOREIGN KEY (gallery_id) REFERENCES gallery (id) ON DELETE CASCADE;

CREATE TABLE gallery_subject
(
	gallery_id BIGINT(20) NOT NULL,
	subject_id BIGINT(20) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_unicode_ci;

ALTER TABLE gallery_subject
	ADD CONSTRAINT gallery_subject_gallery_cstr FOREIGN KEY (gallery_id) REFERENCES gallery (id) ON DELETE CASCADE;
