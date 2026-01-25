CREATE SEQUENCE IF NOT EXISTS web_site_acl_id_seq
	AS BIGINT
	START WITH 1
	INCREMENT BY 1
	NO MINVALUE
	NO MAXVALUE
	CACHE 50;

SELECT setval(
			   'web_site_acl_id_seq',
			   GREATEST(
					   COALESCE((SELECT MAX(acl_id) FROM web_site_file), 0),
					   COALESCE((SELECT MAX(acl_id) FROM web_site_gallery), 0),
					   COALESCE((SELECT MAX(acl_id) FROM web_site_page), 0)));
