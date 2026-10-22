-- The time a resource was last released to the Vempain Website; NULL means it has never been released.
-- Pages already carry page.published (V1000), this adds the same column to the other resources that are published to the site.
ALTER TABLE gallery
    ADD COLUMN published TIMESTAMP DEFAULT NULL;

ALTER TABLE site_file
    ADD COLUMN published TIMESTAMP DEFAULT NULL;

ALTER TABLE data_store
    ADD COLUMN published TIMESTAMP DEFAULT NULL;
