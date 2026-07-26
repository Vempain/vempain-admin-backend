ALTER TABLE web_site_file
	ADD COLUMN thumbnail_path TEXT DEFAULT NULL;

CREATE UNIQUE INDEX ux_web_site_file_thumbnail_path ON web_site_file (thumbnail_path) WHERE thumbnail_path IS NOT NULL;

UPDATE web_site_file
SET thumbnail_path =
		regexp_replace(
				regexp_replace(
						file_path,
					    '/([^/]+)$',
					    '/.thumb/\1'
			    ),
			    E'\\.[^.]+$',
			    '.jpg'
	    )
WHERE thumbnail_path IS NULL;
