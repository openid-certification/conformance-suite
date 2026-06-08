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
class VCIValidateCredentialSigningAlgValuesSupported_UnitTest extends AbstractVciUnitTest {

	private VCIValidateCredentialSigningAlgValuesSupported cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateCredentialSigningAlgValuesSupported();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void rejectsDresdenKommPassMetadataWithMisspelledAlgs() throws Exception {
		String json = readFile("metadata/openid4vci-1_0/credential-issuer-metadata-dresden-komm-pass.json");
		putCredentialIssuerMetadata(json);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsDcSdJwtWithValidAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "credential_signing_alg_values_supported": ["ES256", "RS256"]
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsDcSdJwtWithUnknownAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "credential_signing_alg_values_supported": ["ES265"]
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsDcSdJwtWithMacAlg() {
		// dc+sd-jwt credentials are signed with the issuer's key and verified with the
		// issuer's published public key; a MAC algorithm cannot interoperate.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "credential_signing_alg_values_supported": ["HS256"]
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsDcSdJwtWithIntegerInAlgArray() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "credential_signing_alg_values_supported": [-7]
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsMsoMdocWithValidCoseAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "mso_mdoc",
			      "doctype": "org.iso.18013.5.1.mDL",
			      "credential_signing_alg_values_supported": [-7, -35]
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsMsoMdocWithUnknownCoseAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "mso_mdoc",
			      "doctype": "org.iso.18013.5.1.mDL",
			      "credential_signing_alg_values_supported": [-99]
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsMsoMdocWithStringAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "mso_mdoc",
			      "doctype": "org.iso.18013.5.1.mDL",
			      "credential_signing_alg_values_supported": ["ES256"]
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void noErrorWhenAlgValuesAbsent() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct"
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void skipsConfigsWithUnknownFormat() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "some_future_format",
			      "credential_signing_alg_values_supported": ["this would normally be invalid"]
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
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

	@Test
	void doesNotCrashWhenCredentialConfigurationsSupportedIsNotAnObject() {
		putCredentialIssuerMetadata("""
			{"credential_configurations_supported": []}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonIntegerCoseAlgorithmIdentifier() {
		putCredentialIssuerMetadata("""
			{
			  "credential_configurations_supported": {
			    "cred1": {
			      "format": "mso_mdoc",
			      "credential_signing_alg_values_supported": [-7.9]
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putCredentialIssuerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}
}
