-- Remove the PHP wrapper emitted by the legacy PHP website backend.
UPDATE web_site_page
SET body = regexp_replace(
		regexp_replace(body, '^\?>', ''),
		'<\?php[[:space:]]*$',
		''
		   )
WHERE body ~ '^\?>'
  AND body ~ '<\?php[[:space:]]*$';
