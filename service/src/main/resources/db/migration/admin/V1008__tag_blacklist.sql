-- Tags (subjects) that must never reach the Vempain Website. When a gallery is published, a blacklisted tag is left out of the site
-- file's subjects and removed from the keyword lists of the metadata copied to the site. Matching is case-insensitive on the trimmed name.
CREATE TABLE tag_blacklist
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tag_name   VARCHAR(255) NOT NULL,
    created_by BIGINT       NOT NULL,
    created    TIMESTAMP    NOT NULL,
    FOREIGN KEY (created_by) REFERENCES user_account (id)
);

CREATE UNIQUE INDEX ux_tag_blacklist_tag_name ON tag_blacklist (LOWER(tag_name));
