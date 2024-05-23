package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ComponentRepositoryITC extends AbstractITCTest {
	private final long initCount = 10;

	@Test
	void findAllOk() throws VempainComponentException, VempainAbstractException {
		log.info("================== findAllOk start");
		testITCTools.generateComponents(initCount);
		Iterable<Component> components = componentRepository.findAll();
		assertNotNull(components);
		assertEquals(initCount, StreamSupport.stream(components.spliterator(), false).count());

		for (Component component : components) {
			assertNotNull(component.getCreator());
			assertNotNull(component.getCreated());
			assertNull(component.getModifier());
			assertNull(component.getModified());
			assertTrue(component.getCompName().contains("Test component "));
			assertTrue(component.getCompData().contains("Test component data "));
		}
		log.info("================== findAllOk end");
	}

	@Test
	void findByIdOk() throws VempainComponentException, VempainAbstractException {
		var componentIds = testITCTools.generateComponents(initCount);
		Long                componentId       = componentIds.getFirst();
		Optional<Component> optionalComponent = componentRepository.findById(componentId);
		assertTrue(optionalComponent.isPresent());
		Component component = optionalComponent.get();
		assertNotNull(component);
		assertNotNull(component.getCreator());
		assertNotNull(component.getCreated());
		assertNull(component.getModifier());
		assertNull(component.getModified());
		assertTrue(component.getCompName().startsWith("Test component 1"));
		assertEquals("Test component data 1", component.getCompData());
	}

	@Test
	void findByCompNameOk() throws VempainComponentException, VempainAbstractException {
		var componentIds = testITCTools.generateComponents(initCount);
		Long                componentId       = componentIds.getFirst();
		Optional<Component> optionalComponent = componentRepository.findById(componentId);
		assertTrue(optionalComponent.isPresent());
		Component component  = optionalComponent.get();
		String    compName   = component.getCompName();
		Component component2 = componentRepository.findByCompName(component.getCompName());
		assertNotNull(component2);
		assertNotNull(component2.getCreator());
		assertNotNull(component2.getCreated());
		assertNull(component2.getModifier());
		assertNull(component2.getModified());
		assertEquals(component2.getCompName(), compName);
	}

	@Transactional
	@Test
	void deleteComponentByIdOk() throws VempainComponentException, VempainAbstractException {
		var componentIds = testITCTools.generateComponents(initCount);
		Long                componentId       = componentIds.getFirst();
		Optional<Component> optionalComponent = componentRepository.findById(componentId);
		assertTrue(optionalComponent.isPresent());
		Component component = optionalComponent.get();
		componentRepository.deleteById(component.getId());
		Optional<Component> noComponent = componentRepository.findById(componentId);
		assertTrue(noComponent.isEmpty());
	}
}
