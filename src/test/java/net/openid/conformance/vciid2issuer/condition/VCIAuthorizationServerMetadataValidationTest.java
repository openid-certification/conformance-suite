package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class VCIAuthorizationServerMetadataValidationTest extends AbstractVciUnitTest {

	VCIAuthorizationServerMetadataValidation validation;

	@Mock
	TestInstanceEventLog eventLog;

	Environment env;

	@BeforeEach
	public void setup() {
		validation = new VCIAuthorizationServerMetadataValidation();
		validation.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void shouldReportNoErrorsForMinimalMockMetadata() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/valid-oauth-authorization-server-metadata-mock.json");
		env.putObject("vci", "authorization_servers.server0.authorization_server_metadata", JsonParser.parseString(metadataString).getAsJsonObject());
		validation.evaluate(env);
	}

	@Test
	void shouldReportNoErrorsForFullMockMetadata() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/valid-oauth-authorization-server-metadata-mock-full.json");
		env.putObject("vci", "authorization_servers.server0.authorization_server_metadata", JsonParser.parseString(metadataString).getAsJsonObject());
		validation.evaluate(env);
	}

	@Test
	void shouldReportValidationErrorFoMultipleErrors() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/invalid-oauth-authorization-server-metadata-multiple-errors.json");
		env.putObject("vci", "authorization_servers.server0.authorization_server_metadata", JsonParser.parseString(metadataString).getAsJsonObject());

		Map<String, Object> data = assertValidationError(validation, env, eventLog);
		assertContainsExpectedError(data, "$.response_types_supported", "string found, array expected");
		assertContainsExpectedError(data, "$.issuer", "integer found, string expected");
		assertContainsExpectedError(data, "$.authorized_grant_anonymous_access_supported", "string found, boolean expected");
		assertContainsExpectedError(data, "$.code_challenge_methods_supported[1]", "integer found, string expected");
	}
}
