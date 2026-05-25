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
class VCIValidateProofSigningAlgValuesSupported_UnitTest extends AbstractVciUnitTest {

	private VCIValidateProofSigningAlgValuesSupported cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateProofSigningAlgValuesSupported();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void acceptsJwtProofTypeWithValidAlgs() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "proof_types_supported": {
			        "jwt": {
			          "proof_signing_alg_values_supported": ["ES256", "RS256"]
			        }
			      }
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsJwtProofTypeWithTypoAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "proof_types_supported": {
			        "jwt": {
			          "proof_signing_alg_values_supported": ["ES265"]
			        }
			      }
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsJwtProofTypeWithMacAlg() {
		// OID4VCI 1.0 Final Appendix F requires key proofs to use an asymmetric
		// digital-signature algorithm; HMAC algs are not acceptable.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "proof_types_supported": {
			        "jwt": {
			          "proof_signing_alg_values_supported": ["HS256"]
			        }
			      }
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsAttestationProofTypeWithAsymmetricAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "proof_types_supported": {
			        "attestation": {
			          "proof_signing_alg_values_supported": ["ES256", "EdDSA"]
			        }
			      }
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsAttestationProofTypeWithTypoAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "proof_types_supported": {
			        "attestation": {
			          "proof_signing_alg_values_supported": ["ES265"]
			        }
			      }
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsAttestationProofTypeWithMacAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/vct",
			      "proof_types_supported": {
			        "attestation": {
			          "proof_signing_alg_values_supported": ["HS256"]
			        }
			      }
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void skipsCwtProofTypeForNow() {
		// cwt's spec algs are COSE integer ids, but the current schema types items as strings.
		// This condition silently skips non-jwt proof types until that schema gap is addressed.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "mso_mdoc",
			      "doctype": "org.iso.18013.5.1.mDL",
			      "proof_types_supported": {
			        "cwt": {
			          "proof_signing_alg_values_supported": ["this-would-fail-if-validated"]
			        }
			      }
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void noErrorWhenProofTypesAbsent() {
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
	void rejectsCredentialConfigurationsSupportedMissing() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential"
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

	@Test
	void doesNotCrashWhenCredentialConfigurationsSupportedIsNotAnObject() {
		putCredentialIssuerMetadata("""
			{"credential_configurations_supported": []}
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
