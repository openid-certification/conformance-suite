package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class VCIExtractCredentialResponseTest extends AbstractVciUnitTest {

	VCIExtractCredentialResponse cond;

	@Mock
	TestInstanceEventLog eventLog;

	Environment env;

	@BeforeEach
	public void setup() {
		cond = new VCIExtractCredentialResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void shouldReportNoErrorsForMinimalMockMetadata() throws Exception {
		String metadataString = """
			{
			  "credentials": [
				{
				  "credential": "LUpixVCWJk0eOt4CXQe1NXK....WZwmhmn9OQp6YxX0a2L"
				}
			  ]
			}
			""";
		env.putString("endpoint_response", "body", metadataString);
		cond.evaluate(env);

		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");
		assertThat(list).hasSize(1);
		assertThat(list.get(0).getAsString()).isEqualTo("LUpixVCWJk0eOt4CXQe1NXK....WZwmhmn9OQp6YxX0a2L");
	}

	@Test
	void shouldExtractMultipleCredentials() throws Exception {
		String metadataString = """
			{
			  "credentials": [
				{
				  "credential": "credential_one"
				},
				{
				  "credential": "credential_two"
				},
				{
				  "credential": "credential_three"
				}
			  ]
			}
			""";
		env.putString("endpoint_response", "body", metadataString);
		cond.evaluate(env);

		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");
		assertThat(list).hasSize(3);
		assertThat(list.get(0).getAsString()).isEqualTo("credential_one");
		assertThat(list.get(1).getAsString()).isEqualTo("credential_two");
		assertThat(list.get(2).getAsString()).isEqualTo("credential_three");
	}

	@Test
	void shouldRejectNonObjectEntryInCredentialsArray() {
		String metadataString = """
			{
			  "credentials": [
				"not_an_object"
			  ]
			}
			""";
		env.putString("endpoint_response", "body", metadataString);
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	@Test
	void shouldRejectEntryMissingCredentialProperty() {
		String metadataString = """
			{
			  "credentials": [
				{
				  "other": "value"
				}
			  ]
			}
			""";
		env.putString("endpoint_response", "body", metadataString);
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

}
