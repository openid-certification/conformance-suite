package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIGenerateSignedCredentialIssuerMetadata_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIGenerateSignedCredentialIssuerMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIGenerateSignedCredentialIssuerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_missingSigningJwkErrorReferencesConfigField() {
		env.putObject("config", new JsonObject());
		env.putObject("credential_issuer_metadata", new JsonObject());

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'Signing JWK' field is missing from the 'Credential Issuer' section in the test configuration"),
			"expected message to reference the missing 'Signing JWK' config field but was: " + err.getMessage());
	}

}
