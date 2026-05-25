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
class VCIValidateCredentialConfigurationScopeSyntax_UnitTest extends AbstractVciUnitTest {

	private VCIValidateCredentialConfigurationScopeSyntax cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateCredentialConfigurationScopeSyntax();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void acceptsKommPassScopeWhichIsTechnicallyValid() throws Exception {
		String json = readFile("metadata/openid4vci-1_0/credential-issuer-metadata-dresden-komm-pass.json");
		putCredentialIssuerMetadata(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsTypicalScope() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "scope": "TestCredScope"
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void noErrorWhenScopeAbsent() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt"
			    }
			  }
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsScopeWithSpace() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "scope": "two words"
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsScopeWithDoubleQuote() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "scope": "foo\\"bar"
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsEmptyScope() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "TestCred": {
			      "format": "dc+sd-jwt",
			      "scope": ""
			    }
			  }
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
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
