package fi.poltsi.vempain.admin.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Data
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_unit")
public class UserUnit implements Serializable {
	@EmbeddedId
	private UserUnitId  id;
	@ManyToOne(fetch = FetchType.EAGER)
	@MapsId("userId")
	private UserAccount userAccount;
	@ManyToOne(fetch = FetchType.EAGER)
	@MapsId("unitId")
	private Unit        unit;
}
