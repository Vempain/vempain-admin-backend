package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.Gallery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GalleryRepositoryImpl implements GalleryRepositoryCustom {
	private static final Pattern       TOKEN_PATTERN = Pattern.compile("\"([^\"]+)\"|(\\S+)");
	private final        EntityManager entityManager;

	@Override
	public Page<Gallery> searchGalleries(String searchTerm, boolean caseSensitive, Pageable pageable) {
		List<String> tokens = tokenize(searchTerm);
		String base = """
				FROM gallery g
				LEFT JOIN gallery_file gf ON gf.gallery_id = g.id
				LEFT JOIN (SELECT id, file_name, file_path, NULL AS metadata FROM site_file) sf ON sf.id = gf.site_file_id
				""";

		String whereClause = buildWhereClause(tokens, caseSensitive);
		String orderClause = buildOrderClause(pageable);

		String selectSql = "SELECT DISTINCT g.* " + base + whereClause + orderClause +
						   " OFFSET :offset LIMIT :limit";
		log.debug("Gallery search SQL: {}", selectSql);
		Query dataQuery = entityManager.createNativeQuery(selectSql, Gallery.class);
		bindParameters(dataQuery, tokens, caseSensitive);
		dataQuery.setParameter("offset", (int) pageable.getOffset());
		dataQuery.setParameter("limit", pageable.getPageSize());
		@SuppressWarnings("unchecked")
		List<Gallery> galleries = dataQuery.getResultList();

		String countSql = "SELECT COUNT(DISTINCT g.id) " + base + whereClause;
		Query countQuery = entityManager.createNativeQuery(countSql);
		bindParameters(countQuery, tokens, caseSensitive);
		Number total = (Number) countQuery.getSingleResult();

		return new PageImpl<>(galleries, pageable, total.longValue());
	}

	private void bindParameters(Query query, List<String> tokens, boolean caseSensitive) {
		for (int i = 0; i < tokens.size(); i++) {
			String value = caseSensitive ? tokens.get(i) : tokens.get(i)
																 .toLowerCase();
			query.setParameter("term" + i, "%" + value + "%");
		}
	}

	private String buildWhereClause(List<String> tokens, boolean caseSensitive) {
		if (tokens.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(" WHERE ");
		for (int i = 0; i < tokens.size(); i++) {
			if (i > 0) {
				sb.append(" AND ");
			}
			sb.append('(')
			  .append(like("g.shortname", i, caseSensitive))
			  .append(" OR ")
			  .append(like("g.description", i, caseSensitive))
			  .append(" OR ")
			  .append(like("sf.file_name", i, caseSensitive))
			  .append(" OR ")
			  .append(like("sf.file_path", i, caseSensitive))
			  .append(')');
		}
		return sb.toString();
	}

	private String like(String column, int index, boolean caseSensitive) {
		return (caseSensitive ? column : "LOWER(" + column + ")") + " LIKE :term" + index;
	}

	private String buildOrderClause(Pageable pageable) {
		if (!pageable.getSort()
					 .isSorted()) {
			return " ORDER BY g.id ASC";
		}
		StringBuilder sb = new StringBuilder(" ORDER BY ");
		boolean first = true;
		for (var order : pageable.getSort()) {
			if (!first) {
				sb.append(", ");
			}
			sb.append(mapSort(order.getProperty()))
			  .append(' ')
			  .append(order.getDirection()
						   .name());
			first = false;
		}
		return sb.toString();
	}

	private String mapSort(String property) {
		if (property == null) {
			return "g.id";
		}
		return switch (property.toLowerCase()) {
			case "short_name", "shortname" -> "g.shortname";
			case "description" -> "g.description";
			default -> "g.id";
		};
	}

	private List<String> tokenize(String searchTerm) {
		if (searchTerm == null || searchTerm.isBlank()) {
			return List.of();
		}

		Matcher matcher = TOKEN_PATTERN.matcher(searchTerm);
		List<String> tokens = new ArrayList<>();
		while (matcher.find()) {
			String quoted = matcher.group(1);
			String word = matcher.group(2);
			tokens.add(quoted != null ? quoted : word);
		}
		return tokens;
	}
}
