package net.openid.conformance.vci10issuer.condition;

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
class VCIWarnOnNonCanonicalAuthorizationServerLocales_UnitTest {

	private VCIWarnOnNonCanonicalAuthorizationServerLocales cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIWarnOnNonCanonicalAuthorizationServerLocales();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	void flagsNonCanonicalCase() {
		putServerMetadata("""
			{
			  "ui_locales_supported": ["EN-us"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsCanonicalLocales() {
		putServerMetadata("""
			{
			  "ui_locales_supported": ["en-US", "de-DE"],
			  "claims_locales_supported": ["fr-FR"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void noErrorWhenLocaleArraysAbsent() {
		putServerMetadata("""
			{
			  "issuer": "https://as.example.com"
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void doesNotWarnOnGrandfatheredTagWhoseCanonicalDiffersSubstantively() {
		// art-lojban -> jbo and i-klingon -> tlh are valid registered tags whose canonical form
		// differs in the tag itself, not just in case, so they must not be flagged as a casing issue.
		putServerMetadata("""
			{
			  "ui_locales_supported": ["art-lojban", "i-klingon"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void putServerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("server", metadata);
	}
}
