package fi.poltsi.vempain.tools;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LikePatternsUTC {

	@Test
	void wildcardAndEscapeCharactersAreEscaped() {
		assertEquals("\\%", LikePatterns.escape("%"));
		assertEquals("\\_", LikePatterns.escape("_"));
		assertEquals("\\\\", LikePatterns.escape("\\"));
		assertEquals("50\\% off\\_now", LikePatterns.escape("50% off_now"));
		assertEquals("plain text", LikePatterns.escape("plain text"));
		assertEquals("", LikePatterns.escape(null));
	}

	@Test
	void containsAndPrefixWrapTheEscapedText() {
		assertEquals("%\\%%", LikePatterns.contains("%"));
		assertEquals("%sunset%", LikePatterns.contains("sunset"));
		assertEquals("%sunset%", LikePatterns.containsIgnoreCase("SunSet"));
		assertEquals("%%", LikePatterns.containsIgnoreCase(null));
		assertEquals("trips/2024\\_x%", LikePatterns.prefix("trips/2024_x"));
		assertEquals("%", LikePatterns.prefix(null));
		assertEquals("%%", LikePatterns.contains(null));
	}

	@Test
	void overlongTextIsTruncated() {
		var text = "a".repeat(LikePatterns.MAX_TEXT_LENGTH + 50);
		var pattern = LikePatterns.contains(text);

		assertEquals(LikePatterns.MAX_TEXT_LENGTH + 2, pattern.length());
		assertTrue(pattern.startsWith("%a") && pattern.endsWith("a%"));
	}

	@Test
	void theNumberOfSearchTokensIsBounded() {
		assertEquals(List.of(), LikePatterns.limitTokens(null));

		var few = List.of("a", "b");
		assertSame(few, LikePatterns.limitTokens(few));

		var many = IntStream.range(0, LikePatterns.MAX_TOKENS + 5)
							.mapToObj(Integer::toString)
							.toList();
		var limited = LikePatterns.limitTokens(many);
		assertEquals(LikePatterns.MAX_TOKENS, limited.size());
		assertEquals(many.subList(0, LikePatterns.MAX_TOKENS), limited);
	}
}
