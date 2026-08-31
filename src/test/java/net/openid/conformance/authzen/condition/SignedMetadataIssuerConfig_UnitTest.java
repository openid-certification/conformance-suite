package net.openid.conformance.authzen.condition;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ValidatePDPMetadataIssuer, ValidatePDPSignedMetadataIss and WarnUnusedSignedMetadataIssuer all
 * branch on whether a 'Signed Metadata Issuer' was configured, and would contradict each other if
 * they answered that question differently. These tests pin the shared answer.
 */
class SignedMetadataIssuerConfig_UnitTest {

	private static final String ISSUER = "https://attester.example.com";

	private Environment envWith(Object metadataIssuer) {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		if (metadataIssuer instanceof String s) {
			pdp.addProperty("metadata_issuer", s);
		} else if (metadataIssuer instanceof Number n) {
			pdp.addProperty("metadata_issuer", n);
		} else if (metadataIssuer == JsonNull.INSTANCE) {
			pdp.add("metadata_issuer", JsonNull.INSTANCE);
		}
		config.add("pdp", pdp);
		Environment env = new Environment();
		env.putObject("config", config);
		return env;
	}

	@Test
	public void absent_isNotConfigured() {
		Environment env = envWith(null);
		assertFalse(SignedMetadataIssuerConfig.isConfigured(env));
		assertNull(SignedMetadataIssuerConfig.value(env));
		assertNull(SignedMetadataIssuerConfig.element(env));
	}

	@Test
	public void jsonNull_isNotConfigured() {
		Environment env = envWith(JsonNull.INSTANCE);
		assertFalse(SignedMetadataIssuerConfig.isConfigured(env));
		assertNull(SignedMetadataIssuerConfig.value(env));
	}

	@Test
	public void emptyString_isNotConfigured() {
		// The rule the three conditions must agree on: blank behaves exactly like absent.
		Environment env = envWith("");
		assertFalse(SignedMetadataIssuerConfig.isConfigured(env));
		assertNull(SignedMetadataIssuerConfig.value(env));
	}

	@Test
	public void value_isConfigured() {
		Environment env = envWith(ISSUER);
		assertTrue(SignedMetadataIssuerConfig.isConfigured(env));
		assertEquals(ISSUER, SignedMetadataIssuerConfig.value(env));
	}

	@Test
	public void wrongType_isConfiguredButHasNoStringValue() {
		// Counts as configured so that it is reported rather than silently ignored, and answering that
		// must not throw: WarnUnusedSignedMetadataIssuer asks the question before anything has validated
		// the field's type.
		Environment env = envWith(42);
		assertTrue(SignedMetadataIssuerConfig.isConfigured(env));
		assertNull(SignedMetadataIssuerConfig.value(env));
	}

	@Test
	public void noPdpSection_isNotConfigured() {
		Environment env = new Environment();
		env.putObject("config", new JsonObject());
		assertFalse(SignedMetadataIssuerConfig.isConfigured(env));
		assertNull(SignedMetadataIssuerConfig.value(env));
	}
}
