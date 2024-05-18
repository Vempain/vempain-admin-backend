-- The app_file, app_filesize and app_sha1sum are no longer used in file_common, remove them from the schema

ALTER TABLE file_common DROP COLUMN app_file;
ALTER TABLE file_common DROP COLUMN app_filesize;
ALTER TABLE file_common DROP COLUMN app_sha1sum;
