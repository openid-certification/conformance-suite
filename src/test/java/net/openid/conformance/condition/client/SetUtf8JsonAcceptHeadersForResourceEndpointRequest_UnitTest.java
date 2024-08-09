package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
public class SetUtf8JsonAcceptHeadersForResourceEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetUtf8JsonAcceptHeadersForResourceEndpointRequest cond;

	private final String expectedHeader = "application/json;charset=UTF-8";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		cond = new SetUtf8JsonAcceptHeadersForResourceEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noHeaders() {

		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("resource_endpoint_request_headers", "Accept")).isEqualTo(expectedHeader);
		assertThat(env.getString("resource_endpoint_request_headers", "Accept-Charset")).isEqualTo("UTF-8");
	}

	@Test
	public void testEvaluate_replace() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Accept", "something else");
		env.putObject("resource_endpoint_request_headers", headers);

		cond.execute(env);

		assertThat(env.getString("resource_endpoint_request_headers", "Accept")).isEqualTo(expectedHeader);
		assertThat(env.getString("resource_endpoint_request_headers", "Accept-Charset")).isEqualTo("UTF-8");
	}

}
