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
class VCIValidateEncryptionZipValues_UnitTest extends AbstractVciUnitTest {

	private VCIValidateEncryptionZipValues cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateEncryptionZipValues();
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
	void noErrorWhenZipValuesAbsent() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["RSA-OAEP-256"],
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsDefInResponseEncryptionBlock() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["RSA-OAEP-256"],
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false,
			    "zip_values_supported": ["DEF"]
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsDefInRequestEncryptionBlock() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_request_encryption": {
			    "jwks": {"keys": []},
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false,
			    "zip_values_supported": ["DEF"]
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsUnknownZipValueInResponseBlock() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_response_encryption": {
			    "alg_values_supported": ["RSA-OAEP-256"],
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false,
			    "zip_values_supported": ["DEF", "gzip"]
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsUnknownZipValueInRequestBlock() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_request_encryption": {
			    "jwks": {"keys": []},
			    "enc_values_supported": ["A128GCM"],
			    "encryption_required": false,
			    "zip_values_supported": ["gzip"]
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
