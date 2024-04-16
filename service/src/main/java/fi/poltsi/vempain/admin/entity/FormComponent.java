package fi.poltsi.vempain.admin.entity;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormComponent implements Serializable {
	@Column(name = "sort_order")
	private Long sortOrder;
	@Column(name = "form_id")
	private Long formId;
	@Column(name = "component_id")
	private Long componentId;
}
