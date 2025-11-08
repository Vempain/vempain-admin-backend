package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Layout;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class LayoutRepositoryITC extends AbstractITCTest {
	private final long initCount = 10;

	@Test
	void findAll() {
		testITCTools.generateLayouts(initCount);
		Iterable<Layout> layouts = layoutRepository.findAll();
		assertNotNull(layouts);
		assertEquals(initCount, StreamSupport.stream(layouts.spliterator(), false)
											 .count());

		for (Layout layout : layouts) {
			assertLayout(layout);
		}
	}

	@Test
	void findById() {
		var layoutIds = testITCTools.generateLayouts(initCount);
		Optional<Layout> optionalLayout = layoutRepository.findById(layoutIds.getFirst());
		assertTrue(optionalLayout.isPresent());
		assertLayout(optionalLayout.get());
	}

	@Test
	void findByLayoutName() {
		var layoutIds = testITCTools.generateLayouts(initCount);
		var layout = layoutRepository.findById(layoutIds.getFirst());
		assertTrue(layout.isPresent());
		var layoutName = layout.get()
							   .getLayoutName();
		Optional<Layout> optionalLayout = layoutRepository.findByLayoutName(layoutName);
		assertTrue(optionalLayout.isPresent());
		assertLayout(optionalLayout.get());
	}

	@Test
	void deleteLayoutById() {
		var layoutIds = testITCTools.generateLayouts(initCount);
		var layout = layoutRepository.findById(layoutIds.getFirst());
		assertTrue(layout.isPresent());
		var layoutName = layout.get()
							   .getLayoutName();
		Optional<Layout> optionalLayout = layoutRepository.findByLayoutName(layoutName);
		assertTrue(optionalLayout.isPresent());
		layoutRepository.deleteById(optionalLayout.get()
												  .getId());
		Optional<Layout> optionalLayout1 = layoutRepository.findByLayoutName(layoutName);
		assertTrue(optionalLayout1.isEmpty());
	}

	private void assertLayout(Layout layout) {
		assertNotNull(layout);
		assertNotNull(layout.getCreator());
		assertNotNull(layout.getCreated());

		if (layout.getModifier() != null) {
			assertNull(layout.getModified());
			assertTrue(layout.getCreated()
							 .isBefore(layout.getModified()));
		}

		assertTrue(layout.getId() > 0);
		assertTrue(layout.getLayoutName()
						 .contains("Test layout "));
		assertEquals("<!--comp_0--><!--comp_1--><!--page--><!--comp_2-->", layout.getStructure());
	}
}
