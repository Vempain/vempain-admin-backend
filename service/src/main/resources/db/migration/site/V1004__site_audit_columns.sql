-- Audit trail of the site resources that lacked it: who released or last changed the row and when, like web_site_page carries it.
-- The creator and modifier are the admin user's nick (the website has its own user base, so admin user IDs mean nothing here).
-- Rows published before this release keep NULL until they are published again; the configuration rows seeded by V1000 keep NULL
-- creator/created and get modifier/modified on their next update.
ALTER TABLE web_site_file
    ADD COLUMN creator  VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN created  TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
    ADD COLUMN modifier VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN modified TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL;

ALTER TABLE web_gps_location
    ADD COLUMN creator  VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN created  TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
    ADD COLUMN modifier VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN modified TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL;

ALTER TABLE web_site_subject
    ADD COLUMN creator  VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN created  TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
    ADD COLUMN modifier VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN modified TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL;

ALTER TABLE web_site_configuration
    ADD COLUMN creator  VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN created  TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
    ADD COLUMN modifier VARCHAR(512)                DEFAULT NULL,
    ADD COLUMN modified TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL;

-- web_site_gallery named its creator and modifier by admin user ID, which the website cannot resolve; every other site table names
-- the admin user by nick (V1000 web_site_page, V1004 the rest). The existing IDs are dropped rather than converted: the site database
-- has no access to the admin user base, and a gallery row is rewritten on its next publish anyway.
ALTER TABLE web_site_gallery
    ALTER COLUMN creator DROP NOT NULL,
    ALTER COLUMN creator TYPE VARCHAR(512) USING NULL,
    ALTER COLUMN modifier TYPE VARCHAR(512) USING NULL;
