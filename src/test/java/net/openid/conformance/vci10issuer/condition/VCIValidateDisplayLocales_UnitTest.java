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
class VCIValidateDisplayLocales_UnitTest extends AbstractVciUnitTest {

	private VCIValidateDisplayLocales cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateDisplayLocales();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void rejectsDresdenKommPassMetadataDueToUppercaseLocale() throws Exception {
		String json = readFile("metadata/openid4vci-1_0/credential-issuer-metadata-dresden-komm-pass.json");
		putCredentialIssuerMetadata(json);
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
			    {"name": "Aussteller", "locale": "de-DE"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsLocaleAbsent() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsMalformedLocaleWithUnderscore() {
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
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonCanonicalCase() {
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
	void rejectsUnregisteredLanguage() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "xx"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsUnregisteredRegion() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "en-AB"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsDuplicateLocaleWithinSameArray() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer A", "locale": "de-DE"},
			    {"name": "Issuer B", "locale": "de-DE"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void allowsSameLocaleInDifferentDisplayArrays() {
		// One credential issuer display and one credential metadata display both with locale en-US
		// — they are separate display arrays, so the uniqueness rule applies per-array, not globally.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer", "locale": "en-US"}
			  ],
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "credential_metadata": {
			        "display": [
			          {"name": "Credential", "locale": "en-US"}
			        ]
			      }
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void detectsIssueInClaimDisplay() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "credential_metadata": {
			        "claims": [
			          {
			            "path": ["name"],
			            "display": [
			              {"name": "Name", "locale": "DE"}
			            ]
			          }
			        ]
			      }
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsSpecExampleFixtures() throws Exception {
		for (String fixture : new String[]{
			"metadata/openid4vci-1_0/valid-openid-credential-issuer-metadata-mock-full.json",
			"metadata/openid4vci-1_0/valid-openid-credential-issuer-metadata-spec-DcSdJwt-claims-example.json",
			"metadata/openid4vci-1_0/valid-openid-credential-issuer-metadata-eudiw.json"}) {
			env = new Environment();
			putCredentialIssuerMetadata(readFile(fixture));
			assertDoesNotThrow(() -> cond.execute(env), "fixture should pass: " + fixture);
		}
	}

	private void putCredentialIssuerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}
}
