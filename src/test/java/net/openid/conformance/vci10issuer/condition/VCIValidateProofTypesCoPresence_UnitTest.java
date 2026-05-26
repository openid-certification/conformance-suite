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
class VCIValidateProofTypesCoPresence_UnitTest extends AbstractVciUnitTest {

	private VCIValidateProofTypesCoPresence cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateProofTypesCoPresence();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void acceptsBothAbsent() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {"format": "dc+sd-jwt", "vct": "v"}
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsBothPresent() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "v",
			      "cryptographic_binding_methods_supported": ["jwk"],
			      "proof_types_supported": {"jwt": {"proof_signing_alg_values_supported": ["ES256"]}}
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsBindingPresentProofMissing() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "v",
			      "cryptographic_binding_methods_supported": ["jwk"]
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsProofPresentBindingMissing() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "v",
			      "proof_types_supported": {"jwt": {"proof_signing_alg_values_supported": ["ES256"]}}
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsExplicitNullProofTypesAsAbsent() {
		// JsonObject.has() returns true for explicit JSON null; the co-presence check
		// must treat null as absent so a null doesn't fake the bidirectional pairing.
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "v",
			      "cryptographic_binding_methods_supported": ["jwk"],
			      "proof_types_supported": null
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsExplicitNullBindingMethodsAsAbsent() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "v",
			      "cryptographic_binding_methods_supported": null,
			      "proof_types_supported": {"jwt": {"proof_signing_alg_values_supported": ["ES256"]}}
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
			"metadata/openid4vci-1_0/valid-openid-credential-issuer-metadata-spec-appendix-example.json",
			"metadata/openid4vci-1_0/valid-openid-credential-issuer-metadata-eudiw.json",
			"metadata/openid4vci-1_0/credential-issuer-metadata-dresden-komm-pass.json"}) {
			env = new Environment();
			put(readFile(fixture));
			assertDoesNotThrow(() -> cond.execute(env), "fixture should pass: " + fixture);
		}
	}

	@Test
	void doesNotCrashWhenCredentialConfigurationsSupportedIsNotAnObject() {
		put("""
			{"credential_configurations_supported": []}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void put(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}
}
