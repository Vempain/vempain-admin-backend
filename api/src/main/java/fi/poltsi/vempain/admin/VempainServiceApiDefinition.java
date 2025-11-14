package fi.poltsi.vempain.admin;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.experimental.UtilityClass;

@OpenAPIDefinition(info = @Info(version = "${vempain.version}", title = "Vempain Admin Service REST endpoints"),
				   servers = @Server(url = "http://localhost:${server.port}/api", description = "current server"))
@UtilityClass
public class VempainServiceApiDefinition {
}
