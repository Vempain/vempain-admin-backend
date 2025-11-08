package fi.poltsi.vempain.tools;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TemplateTools {
	public static String processTemplateFile(String templateFile, Map<String, String> replacementMap) throws IOException {
		// Load the template file using Spring's ClassPathResource
		ClassPathResource resource = new ClassPathResource(templateFile);

		try (InputStream inputStream = resource.getInputStream()) {
			// Read the content of the template file into a string
			byte[] fileBytes = FileCopyUtils.copyToByteArray(inputStream);
			String templateContent = new String(fileBytes, StandardCharsets.UTF_8);

			for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
				// Replace all occurrences of the key with the value in the template content
				templateContent = templateContent.replace("[[" + entry.getKey() + "]]", entry.getValue());
			}

			return templateContent;
		}
	}
}
