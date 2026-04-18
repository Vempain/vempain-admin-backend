package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.DataEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class DataRepositoryITC extends AbstractITCTest {
	@Autowired
	private DataRepository dataRepository;

	@Test
	void saveAndFindByIdOk() {
		var entity = buildDataEntity("test_repo_find");
		var saved = dataRepository.save(entity);

		assertNotNull(saved.getId());
		Optional<DataEntity> found = dataRepository.findById(saved.getId());
		assertTrue(found.isPresent());
		assertEquals("test_repo_find", found.get().getIdentifier());
		assertEquals("tabulated", found.get().getType());
		assertEquals("Test description", found.get().getDescription());
		assertNotNull(found.get().getCreatedAt());
		assertNotNull(found.get().getUpdatedAt());
	}

	@Test
	void findByIdentifierOk() {
		var entity = buildDataEntity("test_repo_by_identifier");
		dataRepository.save(entity);

		Optional<DataEntity> found = dataRepository.findByIdentifier("test_repo_by_identifier");
		assertTrue(found.isPresent());
		assertEquals("test_repo_by_identifier", found.get().getIdentifier());
	}

	@Test
	void findByIdentifierNotFoundOk() {
		Optional<DataEntity> found = dataRepository.findByIdentifier("nonexistent_identifier");
		assertTrue(found.isEmpty());
	}

	@Test
	void existsByIdentifierTrueOk() {
		var entity = buildDataEntity("test_repo_exists");
		dataRepository.save(entity);

		assertTrue(dataRepository.existsByIdentifier("test_repo_exists"));
	}

	@Test
	void existsByIdentifierFalseOk() {
		assertFalse(dataRepository.existsByIdentifier("nonexistent_identifier_xyz"));
	}

	@Test
	void findAllOk() {
		dataRepository.save(buildDataEntity("test_findall_1"));
		dataRepository.save(buildDataEntity("test_findall_2"));
		dataRepository.save(buildDataEntity("test_findall_3"));

		Iterable<DataEntity> all = dataRepository.findAll();
		long count = StreamSupport.stream(all.spliterator(), false).count();
		assertTrue(count >= 3);
	}

	@Test
	void deleteByIdOk() {
		var entity = buildDataEntity("test_repo_delete");
		var saved = dataRepository.save(entity);
		Long id = saved.getId();

		dataRepository.deleteById(id);

		Optional<DataEntity> deleted = dataRepository.findById(id);
		assertTrue(deleted.isEmpty());
	}

	@Test
	void updateEntityOk() {
		var entity = buildDataEntity("test_repo_update");
		var saved = dataRepository.save(entity);

		saved.setDescription("Updated description");
		saved.setType("time_series");
		saved.setUpdatedAt(Instant.now());
		var updated = dataRepository.save(saved);

		assertEquals("Updated description", updated.getDescription());
		assertEquals("time_series", updated.getType());
	}

	private DataEntity buildDataEntity(String identifier) {
		var now = Instant.now();
		return DataEntity.builder()
						 .identifier(identifier)
						 .type("tabulated")
						 .description("Test description")
						 .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"}]")
						 .createSql("CREATE TABLE website_data__" + identifier + " (id BIGSERIAL PRIMARY KEY, title VARCHAR(255), year INTEGER)")
						 .fetchAllSql("SELECT * FROM website_data__" + identifier + " ORDER BY id")
						 .fetchSubsetSql("SELECT * FROM website_data__" + identifier + " WHERE year = :year")
						 .generated(now)
						 .csvData("title,year\nTest Album,2024")
						 .createdAt(now)
						 .updatedAt(now)
						 .build();
	}
}
