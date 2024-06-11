package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

import static jakarta.persistence.EnumType.STRING;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "publish_schedule")
public class PublishSchedule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@NotNull
	@Column(name = "publish_time", nullable = false)
	private Instant publishTime;

	@Size(max = 255)
	@NotNull
	@Column(name = "publish_status", nullable = false)
	@Enumerated(STRING)
	private PublishStatusEnum publishStatus;

	@Column(name = "publish_message", length = Integer.MAX_VALUE)
	private String publishMessage;

	@Size(max = 255)
	@NotNull
	@Column(name = "publish_type", nullable = false)
	@Enumerated(STRING)
	private ContentTypeEnum publishType;

	@NotNull
	@Column(name = "publish_id", nullable = false)
	private Long publishId;

	@NotNull
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@NotNull
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public PublishScheduleResponse toResponse() {
		return PublishScheduleResponse.builder()
									  .id(id)
									  .publishTime(publishTime)
									  .publishStatus(publishStatus)
									  .publishMessage(publishMessage)
									  .publishType(publishType)
									  .publishId(publishId)
									  .createdAt(createdAt)
									  .updatedAt(updatedAt)
									  .build();
	}
}
