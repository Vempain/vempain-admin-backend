package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.FormComponent;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class FormComponentService {
	private final EntityManager entityManager;

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteFormComponent(Long formId, Long componentId, Long sortOrder) {
		var query = entityManager.createNativeQuery("DELETE FROM form_component " +
													"WHERE form_id = :formId " +
													"  AND component_id = :componentId " +
													"  AND sort_order = :sortOrder");
		query.setParameter("formId", formId);
		query.setParameter("componentId", componentId);
		query.setParameter("sortOrder", sortOrder);
		query.executeUpdate();
	}

	public List<FormComponent> findFormComponentByFormId(Long formId) {
		var query = entityManager.createNativeQuery("SELECT fc.form_id, fc.component_id, fc.sort_order " +
													"FROM form_component fc " +
													"WHERE fc.form_id = :formId " +
													"ORDER BY fc.sort_order");
		query.setParameter("formId", formId);
		List<Object[]> fcObjects = query.getResultList();
		return mapComponentFormResults(fcObjects);
	}

	public List<FormComponent> findFormComponentByComponentId(long componentId) {

		var query = entityManager.createNativeQuery("SELECT fc.form_id, fc.component_id, fc.sort_order " +
													"FROM form_component fc " +
													"WHERE fc.component_id = :componentId");
		query.setParameter("componentId", componentId);
		List<Object[]> fcObjects = query.getResultList();

		return mapComponentFormResults(fcObjects);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addFormComponent(Long formId, Long componentId, Long sortOrder) {
		var query = entityManager.createNativeQuery("INSERT INTO form_component (form_id, component_id, sort_order) " +
													"VALUES (:formId, :componentId, :sortOrder)");
		query.setParameter("formId", formId);
		query.setParameter("componentId", componentId);
		query.setParameter("sortOrder", sortOrder);
		query.executeUpdate();
	}

	private List<FormComponent> mapComponentFormResults(List<Object[]> fcObjects) {
		var formComponents = new ArrayList<FormComponent>();

		if (!fcObjects.isEmpty()) {
			for (Object[] o : fcObjects) {
				var formId = (Long) o[0];
				var componentId = (Long) o[1];
				var sortOrder = (Long) o[2];
				formComponents.add(FormComponent.builder()
												.formId(formId)
												.componentId(componentId)
												.sortOrder(sortOrder)
												.build());
			}
		}

		return formComponents;
	}
}
