package fi.poltsi.vempain.tools;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateToolsUTC {
	@Test
	void everyOccurrenceOfEveryPlaceholderIsReplaced() throws Exception {
		var replacements = new LinkedHashMap<String, String>();
		replacements.put("name", "Pauli");
		replacements.put("site", "vempain.example");

		var result = TemplateTools.processTemplateFile("templates/greeting.tpl", replacements);

		assertEquals("Hello Pauli, welcome to vempain.example.\nSigned, Pauli.\n", result);
	}

	@Test
	void unknownPlaceholdersAreLeftAsTheyAre() throws Exception {
		var result = TemplateTools.processTemplateFile("templates/greeting.tpl", Map.of("name", "Pauli"));

		assertEquals("Hello Pauli, welcome to [[site]].\nSigned, Pauli.\n", result);
	}

	@Test
	void aMissingTemplateIsAnIoException() {
		assertThrows(IOException.class, () -> TemplateTools.processTemplateFile("templates/missing.tpl", Map.of()));
	}

	@Test
	void theUtilityCanBeInstantiated() {
		assertNotNull(new TemplateTools());
	}
}
