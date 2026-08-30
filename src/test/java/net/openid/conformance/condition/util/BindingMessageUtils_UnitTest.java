package net.openid.conformance.condition.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BindingMessageUtils_UnitTest {

	@Test
	void acceptsOrdinaryDottedMessagesWithSlashes() {
		assertAll(
			() -> assertFalse(BindingMessageUtils.containsUrl("R$ 1.000/mes")),
			() -> assertFalse(BindingMessageUtils.containsUrl("10.5/10")),
			() -> assertFalse(BindingMessageUtils.containsUrl("v1.2/final")),
			() -> assertFalse(BindingMessageUtils.containsUrl("Confirme em banco.com.br")),
			() -> assertFalse(BindingMessageUtils.containsUrl("Confirme em 192.0.2.1"))
		);
	}

	@Test
	void describesAndSanitizesUrlMatches() {
		assertMatch(
			"Review https://user:password@example.test/consent?account=customer-42",
			"https://user:password@example.test/consent?account=customer-42",
			"absolute_url",
			"https://example.test/[redacted]");
		assertMatch(
			"Contact mailto:support@example.test",
			"mailto:support@example.test",
			"mailto",
			"mailto:[redacted]");
		assertMatch(
			"Review www.example.test/consent",
			"www.example.test/consent",
			"www",
			"www.example.test/[redacted]");
		assertMatch(
			"Review example.test/consent",
			"example.test/consent",
			"host_path",
			"example.test/[redacted]");
		assertMatch(
			"Review ssh://example.test/consent",
			"example.test/consent",
			"host_path",
			"example.test/[redacted]");
	}

	private void assertMatch(String message, String url, String type, String sanitizedValue) {
		BindingMessageUtils.UrlMatch match = BindingMessageUtils.findUrl(message).orElseThrow();
		assertAll(
			() -> assertEquals(type, match.type()),
			() -> assertEquals(message.indexOf(url), match.start()),
			() -> assertEquals(url.length(), match.length()),
			() -> assertEquals(sanitizedValue, match.sanitizedValue())
		);
	}
}
