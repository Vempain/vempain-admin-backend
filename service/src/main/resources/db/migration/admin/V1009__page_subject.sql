-- Tags (subjects) of pages, the page side of what file_subject is for site files. Published to the site as web_site_page_subject rows.
CREATE TABLE page_subject
(
    page_id    BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (page_id, subject_id),
    FOREIGN KEY (page_id) REFERENCES page (id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE CASCADE
);
