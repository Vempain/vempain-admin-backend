package fi.poltsi.vempain.admin;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.experimental.UtilityClass;

@OpenAPIDefinition(info = @Info(version = "${info.build.version}", title = "Vempain Service REST endpoints"),
				   servers = @Server(url = "http://localhost:8080/api", description = "current server"))
@UtilityClass
public class VempainServiceApiDefinition {
}
