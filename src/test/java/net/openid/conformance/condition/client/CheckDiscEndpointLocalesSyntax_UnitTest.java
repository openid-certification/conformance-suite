package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CheckDiscEndpointLocalesSyntax_UnitTest {

	private CheckDiscEndpointLocalesSyntax cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new CheckDiscEndpointLocalesSyntax();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void noErrorWhenBothFieldsAbsent() {
		putServerMetadata("""
			{
			  "issuer": "https://as.example.com"
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsValidUiLocales() {
		putServerMetadata("""
			{
			  "ui_locales_supported": ["en", "en-US", "de", "zh-Hant"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsValidClaimsLocales() {
		putServerMetadata("""
			{
			  "claims_locales_supported": ["fr-CA"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsMalformedTag() {
		putServerMetadata("""
			{
			  "ui_locales_supported": ["not a locale!"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsNonCanonicalCaseHere() {
		// Per RFC 5646 §2.1.1 language tags are case-insensitive; canonical casing is a
		// convention. CheckDiscEndpointLocalesCanonicalCasing surfaces the warning.
		putServerMetadata("""
			{
			  "ui_locales_supported": ["EN-us"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsUnregisteredLanguage() {
		putServerMetadata("""
			{
			  "ui_locales_supported": ["xx"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsUnregisteredRegion() {
		putServerMetadata("""
			{
			  "claims_locales_supported": ["en-AB"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonStringEntry() {
		putServerMetadata("""
			{
			  "ui_locales_supported": [42]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonArray() {
		putServerMetadata("""
			{
			  "ui_locales_supported": "en"
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsExplicitNull() {
		// Some OPs (e.g. Authlete) set unsupported optional fields to JSON null rather than omitting
		// them; null is not a valid array and is non-conformant, so it must be flagged, not accepted.
		putServerMetadata("""
			{
			  "ui_locales_supported": null
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putServerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("server", metadata);
	}
}
