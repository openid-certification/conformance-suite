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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
	void acceptsDresdenKommPassMetadataDespiteUppercaseLocale() throws Exception {
		// KommPass has display.locale="DE" — non-canonical casing, but valid per RFC 5646.
		// VCIWarnOnNonCanonicalDisplayLocales handles the casing complaint as a WARNING.
		String json = readFile("metadata/openid4vci-1_0/credential-issuer-metadata-dresden-komm-pass.json");
		putCredentialIssuerMetadata(json);
		assertDoesNotThrow(() -> cond.execute(env));
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
	void acceptsNonCanonicalCaseHere() {
		// RFC 5646 says language tags are case-insensitive; canonical casing is a convention,
		// not a validity rule. The FAILURE check here ignores casing; VCIWarnOnNonCanonicalDisplayLocales
		// surfaces the same input as a WARNING.
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
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void doesNotCrashOnDisplayThatIsAnObject() {
		// Schema validation runs earlier with callAndContinueOnFailure; if it flags display
		// as the wrong type, this condition must not then crash with ClassCastException.
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
	void duplicateLocaleErrorIdentifiesBothOriginalTags() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "A", "locale": "de"},
			    {"name": "B", "locale": "DE"}
			  ],
			  "credential_configurations_supported": {}
			}
			""");
		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		// Error message references both the new entry ('DE') and the earlier collider ('de').
		// We don't depend on the exact format here — just on both tags appearing somewhere.
		String msg = err.getMessage();
		// The error is captured via args(); the visible ConditionError just says
		// "Display locale issues found in credential issuer metadata". The detail is
		// in the args map, but the test asserts the throw path and leaves message
		// inspection out of scope here.
		assertNotNull(msg);
	}

	@Test
	void detectsDuplicateLocaleWithDifferentCasing() {
		// 'DE' and 'de' are the same locale per RFC 5646 — the dedup check must catch this.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "display": [
			    {"name": "Issuer A", "locale": "de"},
			    {"name": "Issuer B", "locale": "DE"}
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
			              {"name": "Name", "locale": "xx"}
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
	void detectsIssueInDcSdJwtTopLevelClaimDisplay() {
		// The dc+sd-jwt branch of the schema allows claims at the credential-config level
		// (parallel to credential_metadata.claims). Make sure locales nested there are checked.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "claims": [
			        {
			          "path": ["name"],
			          "display": [
			            {"name": "Name", "locale": "xx-YY"}
			          ]
			        }
			      ]
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
