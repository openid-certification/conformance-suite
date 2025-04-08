package net.openid.conformance.vciid2issuer.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(env.getString("credential")).isEqualTo("LUpixVCWJk0eOt4CXQe1NXK....WZwmhmn9OQp6YxX0a2L");
	}

}
