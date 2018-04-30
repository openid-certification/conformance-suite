package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class CallDynamicRegistrationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject requestParameters = new JsonParser().parse("{"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"grant_type\":\"authorization-code\","
		+ "\"redirect_uris\":[\"https://redirecturi.com/\"]"
		+ "}").getAsJsonObject();

	private static JsonObject goodResponse = new JsonParser().parse("{" +
		"\"client_id\":\"9d05956d-5ae5-40ab-86e7-0b6e29144a27\"," +
		"\"client_secret\":\"VkPL2ySvWec0DL4tBbQk--D6kxaWvSlWQlEewOZvEMPidPC4fWp71c-7NdIZ2MJgBpc_zp-l-AXvv073Nfm2ow\"," +
		"\"client_secret_expires_at\":0," +
		"\"client_id_issued_at\":1525119671," +
		"\"registration_access_token\":\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ." +
		"eyJhdWQiOiI5ZDA1OTU2ZC01YWU1LTQwYWItODZlNy0wYjZlMjkxNDRhMjciLCJpc3MiOiJodHRwczpcL1wvbWl0cmVpZC5vcmdcLyIsImlhdCI6MTUyNTExOTY3MSwianRpIjoiMzY3ZGMyMTctOGI1YS00NzU0LWEzM2YtZDI2YmRlZWE4NmY4In0." +
		"V9CpbJe9N7aeM4G0YcdDEih85cZqKD5KJe8nrxCf7FB2Er_3QeAWgVcany1Wb7m_BAwXlJY9SIqRyqrSNiJ844gh8Wd_s14LWv9VPIrZG5JejP-vB1TmxJSRC2B66b2nI0fQ_fuPnEPFuwTwhzpKjtMRdOnlHwXBF9UIXWW-Bf0D7BtvBaJLTRi6TzKIDxYBWPHwU7hrGGXN-ZJKVYLxud_wRZMujXyaT3TRTGlr00wv_MFjoum4PzQnTjTWvftTmV_Y1HtssO89teQMeMa-DjLEbwJ8hPqkRfnO3XFrF7R9AH65V6WWTCKnYbcPePMxrV99p3gWagqVx8eddHUyYQ\"," +
		"\"registration_client_uri\":\"https://mitreid.org/register/9d05956d-5ae5-40ab-86e7-0b6e29144a27\"," +
		"\"redirect_uris\":[\"https://redirecturi.com/\"]," +
		"\"client_name\":\"UNIT-TEST client\"," +
		"\"token_endpoint_auth_method\":\"client_secret_basic\"," +
		"\"scope\":\"openid email profile\"," +
		"\"grant_types\":[\"authorization_code\"]," +
		"\"response_types\":[\"code\"]} ").getAsJsonObject();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("good.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(success(goodResponse.toString(), "application/json")),
		service("error.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(badRequest()),
		service("bad.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(success("This is not JSON!", "text/plain"))));

	private CallDynamicRegistrationEndpoint cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallDynamicRegistrationEndpoint("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"registration_endpoint\":\"https://good.example.com/registration\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("dynamic_registration_request", requestParameters);


		cond.evaluate(env);

		hoverfly.verify(service("good.example.com")
			.post("/registration")
			.body(requestParameters.toString()));

		verify(env, atLeastOnce()).getString("server", "registration_endpoint");

		assertThat(env.get("dynamic_registration_response")).isInstanceOf(JsonObject.class);
		assertThat(env.get("dynamic_registration_response").entrySet()).containsAll(goodResponse.entrySet());
	}
}
