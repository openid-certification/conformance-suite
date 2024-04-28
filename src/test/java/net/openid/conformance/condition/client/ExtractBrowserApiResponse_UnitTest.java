package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ExtractBrowserApiResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private String goodResponse;

	private ExtractBrowserApiResponse cond;


	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractBrowserApiResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodResponse = """
				{
				  "data": {
				    "vp_token": "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOBo<...truncated...>"
				  },
				  "protocol": "openid4vp"
				}""";
	}

	@Test
	public void testEvaluate_goodResponse() {

		env.putString("incoming_request", "body", goodResponse);

		cond.execute(env);

		assertThat(env.getObject("authorization_endpoint_response")).isNotNull();
		assertThat(env.getString("authorization_endpoint_response", "vp_token")).isNotNull();
	}

}
