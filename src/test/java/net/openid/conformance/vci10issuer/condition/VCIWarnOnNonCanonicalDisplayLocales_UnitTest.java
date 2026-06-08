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
class VCIWarnOnNonCanonicalDisplayLocales_UnitTest extends AbstractVciUnitTest {

	private VCIWarnOnNonCanonicalDisplayLocales cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIWarnOnNonCanonicalDisplayLocales();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	void flagsUppercaseLanguageSubtag() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "DE"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void flagsLowercaseRegionSubtag() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "de-de"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsCanonicalLocales() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "en-US"},
			    {"name": "Aussteller", "locale": "de-DE"},
			    {"name": "name3", "locale": "zh-Hans-CN"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void ignoresMalformedTagsLeavingThemToTheFailureCondition() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "de_DE"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void doesNotFlagGrandfatheredTagAsCasingIssue() {
		// i-klingon is a registered IANA grandfathered tag; Java canonicalises it to 'tlh'.
		// The substantive change is the tag itself, not casing — the warn condition should
		// keep quiet rather than mislabel it as a casing problem.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "i-klingon"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void doesNotCrashOnDisplayThatIsAnObject() {
		// Schema FAILURE would flag the type error earlier; this condition must not crash.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": {},
			  "credential_configurations_supported": {}
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void flagsDresdenKommPassExample() throws Exception {
		String json = readFile("metadata/openid4vci-1_0/credential-issuer-metadata-dresden-komm-pass.json");
		putCredentialIssuerMetadata(json);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putCredentialIssuerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}
}
