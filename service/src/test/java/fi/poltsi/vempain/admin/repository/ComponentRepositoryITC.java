package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
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

	@AfterEach
	void tearDown() throws VempainEntityNotFoundException {
		testITCTools.deleteComponents();
		testITCTools.checkDatabase();
	}

	@Test
	void findAllOk() throws VempainComponentException, VempainAbstractException {
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
	}

	@Test
	void findByIdOk() throws VempainComponentException, VempainAbstractException {
		testITCTools.generateComponents(initCount);
		Long                componentId       = testITCTools.getComponentIdList().get(0);
		Optional<Component> optionalComponent = componentRepository.findById(componentId);
		assertTrue(optionalComponent.isPresent());
		Component component = optionalComponent.get();
		assertNotNull(component);
		log.info("Component: {}", component);
		assertNotNull(component.getCreator());
		assertNotNull(component.getCreated());
		assertNull(component.getModifier());
		assertNull(component.getModified());
		assertEquals("Test component 1", component.getCompName());
		assertEquals("Test component data 1", component.getCompData());
	}

	@Test
	void findByCompNameOk() throws VempainComponentException, VempainAbstractException {
		testITCTools.generateComponents(initCount);
		Long                componentId       = testITCTools.getComponentIdList().get(0);
		Optional<Component> optionalComponent = componentRepository.findById(componentId);
		assertTrue(optionalComponent.isPresent());
		Component component  = optionalComponent.get();
		String    compName   = component.getCompName();
		Component component2 = componentRepository.findByCompName(component.getCompName());
		assertNotNull(component2);
		log.info("Component: {}", component2);
		assertNotNull(component2.getCreator());
		assertNotNull(component2.getCreated());
		assertNull(component2.getModifier());
		assertNull(component2.getModified());
		assertEquals(component2.getCompName(), compName);
	}

	@Transactional
	@Test
	void deleteComponentByIdOk() throws VempainComponentException, VempainAbstractException {
		testITCTools.generateComponents(initCount);
		Long                componentId       = testITCTools.getComponentIdList().get(0);
		Optional<Component> optionalComponent = componentRepository.findById(componentId);
		assertTrue(optionalComponent.isPresent());
		Component component = optionalComponent.get();
		componentRepository.deleteById(component.getId());
		Optional<Component> noComponent = componentRepository.findById(componentId);
		assertTrue(noComponent.isEmpty());
	}
}
