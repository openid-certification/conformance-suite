package net.openid.conformance.condition.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BindingMessageUtils_UnitTest {

	@Test
	void acceptsOrdinaryDottedMessagesWithSlashes() {
		assertAll(
			() -> assertFalse(BindingMessageUtils.containsUrl("R$ 1.000/mes")),
			() -> assertFalse(BindingMessageUtils.containsUrl("10.5/10")),
			() -> assertFalse(BindingMessageUtils.containsUrl("v1.2/final"))
		);
	}
}
