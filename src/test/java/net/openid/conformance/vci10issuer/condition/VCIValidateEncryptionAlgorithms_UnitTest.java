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
class VCIValidateEncryptionAlgorithms_UnitTest extends AbstractVciUnitTest {

	private VCIValidateEncryptionAlgorithms cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateEncryptionAlgorithms();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void noErrorWhenEncryptionBlocksAbsent() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential"
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsResponseEncryptionWithValidAlgAndEnc() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["ECDH-ES", "RSA-OAEP-256"],
			    "enc_values_supported": ["A128GCM", "A256CBC-HS512"],
			    "encryption_required": false
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsResponseEncryptionWithUnknownAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["RSA-OAEPxx"],
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsResponseEncryptionWithUnknownEnc() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["RSA-OAEP-256"],
			    "enc_values_supported": ["A128GCMxx"],
			    "encryption_required": false
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsRequestEncryptionWithValidEnc() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_request_encryption": {
			    "jwks": {"keys": []},
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsRequestEncryptionWithUnknownEnc() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_request_encryption": {
			    "jwks": {"keys": []},
			    "enc_values_supported": ["A128GCMxx"],
			    "encryption_required": false
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsResponseEncryptionWithSymmetricAlg() {
		// dir and AES key-wrap require pre-shared symmetric keys; credential response
		// encryption flows publish a public JWK and cannot interoperate with these.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["A128KW"],
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsResponseEncryptionWithDirAlg() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["dir"],
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsXc20pAsEncValue() {
		// XC20P is a Nimbus extra but isn't in the IANA JWA registry; flag it.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["RSA-OAEP-256"],
			    "enc_values_supported": ["XC20P"],
			    "encryption_required": false
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
