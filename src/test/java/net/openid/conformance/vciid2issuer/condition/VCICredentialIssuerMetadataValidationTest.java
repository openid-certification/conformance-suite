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
class VCICredentialIssuerMetadataValidationTest extends AbstractVciUnitTest {

	VCICredentialIssuerMetadataValidation validation;

	@Mock
	TestInstanceEventLog eventLog;

	Environment env;

	@BeforeEach
	public void setup() {
		validation = new VCICredentialIssuerMetadataValidation();
		validation.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void shouldReportNoErrorsForMinimalMockMetadata() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/valid-openid-credential-issuer-metadata-mock-minimal.json");
		env.putObject("vci", "credential_issuer_metadata", JsonParser.parseString(metadataString).getAsJsonObject());
		validation.evaluate(env);
	}

	@Test
	void shouldReportNoErrorsForFullMockMetadata() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/valid-openid-credential-issuer-metadata-mock-full.json");
		env.putObject("vci", "credential_issuer_metadata", JsonParser.parseString(metadataString).getAsJsonObject());
		validation.evaluate(env);
	}

	@Test
	void shouldValidateCorrectedEudiwMetadata() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/valid-openid-credential-issuer-metadata-eudiw.json");
		env.putObject("vci", "credential_issuer_metadata", JsonParser.parseString(metadataString).getAsJsonObject());
		validation.evaluate(env);
	}

	@Test
	void shouldReportValidationErrorForIncorrectEbsiMetadata() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/invalid-openid-credential-issuer-metadata-ebsi.json");
		env.putObject("vci", "credential_issuer_metadata", JsonParser.parseString(metadataString).getAsJsonObject());

		Map<String, Object> data = assertValidationError(validation, env, eventLog);
		assertContainsExpectedError(data,  "$.credential_configurations_supported","required property 'credential_configurations_supported' not found");
	}

	@Test
	void shouldReportValidationErrorForMissingRequiredProperty() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/invalid-openid-credential-issuer-metadata-missing-required-property.json");
		env.putObject("vci", "credential_issuer_metadata", JsonParser.parseString(metadataString).getAsJsonObject());

		Map<String, Object> data = assertValidationError(validation, env, eventLog);
		assertContainsExpectedError(data, "$.credential_endpoint", "required property 'credential_endpoint' not found");
	}

	@Test
	void shouldReportValidationErrorForInvalidPropertyType() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/invalid-openid-credential-issuer-metadata-invalid-type-for-authorization-servers.json");
		env.putObject("vci", "credential_issuer_metadata", JsonParser.parseString(metadataString).getAsJsonObject());

		Map<String, Object> data = assertValidationError(validation, env, eventLog);
		assertContainsExpectedError(data, "$.authorization_servers", "string found, array expected");
	}

	@Test
	void shouldReportValidationErrorForMissingRequiredPropertyInOptionalProperty() throws Exception {
		String metadataString = readFile("metadata/openid4vci-id2/invalid-openid-credential-issuer-metadata-missing-required-property-in-optional-property.json");
		env.putObject("vci", "credential_issuer_metadata", JsonParser.parseString(metadataString).getAsJsonObject());

		Map<String, Object> data = assertValidationError(validation, env, eventLog);
		assertContainsExpectedError(data, "$.credential_response_encryption.encryption_required", "required property 'encryption_required' not found");
		assertContainsExpectedError(data, "$.credential_response_encryption.enc_values_supported", "required property 'enc_values_supported' not found");
	}
}
