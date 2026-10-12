-- Page-gallery links are no longer edited by hand: the admin frontend derives them from the gallery and hero carousel embeds of the page
-- body and saves them with the page (PageService.linkGalleries). Rebuild the links of every page from its body once, so that pages saved
-- before this change carry the same links the editor would produce. Galleries that no longer exist are skipped, a gallery embedded
-- several times is linked once at its first position, and the order follows the body.
DELETE
FROM page_gallery;

INSERT INTO page_gallery (page_id, gallery_id, sort_order)
SELECT linked.page_id,
	   linked.gallery_id,
	   ROW_NUMBER() OVER (PARTITION BY linked.page_id ORDER BY linked.first_position) - 1
FROM (SELECT embed.page_id,
			 embed.gallery_id,
			 MIN(embed.position) AS first_position
	  FROM (SELECT p.id                                       AS page_id,
				   COALESCE(r.groups[1], r.groups[2])::BIGINT AS gallery_id,
				   r.position
			FROM page p
					 CROSS JOIN LATERAL regexp_matches(p.body,
													   '<!--vps:embed:(?:gallery:(\d+)|hero:(\d+):type:carousel)(?::[^>]*)?-->',
													   'g') WITH ORDINALITY AS r(groups, position)) embed
			   JOIN gallery g ON g.id = embed.gallery_id
	  GROUP BY embed.page_id, embed.gallery_id) linked;
