package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractExpiresInFromTokenEndpointResponse_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodResponse;

	private JsonObject badResponse;

	private ExtractExpiresInFromTokenEndpointResponse cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractExpiresInFromTokenEndpointResponse();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodResponse = JsonParser.parseString("{"
			+ "\"access_token\":"
			+ "\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
			+ "eyJzdWIiOiJhZG1pbiIsImF6cCI6ImNsaWVudCIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU5NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6Ijc3MmJiODBhLWQ2ZTEtNGUwMi1hN2M3LTAxNWMwZGQyMjdkNSJ9."
			+ "OsuhXSzoODk6VdUjPSyq9vp_ZLqgNczSF1Gvz8-wh1mGdN1aadWy2faKyqIXoQ7OrOycNkOigmyhYoGYoqM30SmkSm9bmpoTOxCAQDclPRdTjmV_-cF9bRLTvaz-78xOcotk4c9kJQHPvUdXiistvnUywME80rKr5K4m4oAYrv-Ii6ntXK6Wbatx3wjEE8Erkq1dkBO9FKCjqIpVeTM-106uFD6yw4sRZZOgY9ibzovp9522l3VkjaU88UEjTu7WbSaLLyaEgiBVSDoweTogEzI-BIZhjtkaxiDzgKvXUJFF8jxyU-2YB4wgs5oi6SUkWJk825I3Lk2v4EZjE3pZag\","
			+ "\"token_type\":\"Bearer\","
			+ "\"expires_in\":3599,"
			+ "\"scope\":\"address phone openid email profile\","
			+ "\"id_token\":\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
			+ "eyJzdWIiOiI5MDM0Mi5BU0RGSldGQSIsImF1ZCI6ImNsaWVudCIsImF1dGhfdGltZSI6MTUwNjQ1NDIyNywia2lkIjoicnNhMSIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU2NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6IjM2YzU4M2M5LTE1M2UtNDBhOC05M2MzLWEzNWZkYTgwM2QzOCJ9."
			+ "SuGDMrdIh_tCsoWt51xs7rs036KAL3OcwdTcJxmuEUT24EufZILQ9_2rNX8BLA9S-YwYkS243oFz9UfBmnnqj6H27-BO7yBSwGnofDwV6GN4yLXmJfrzC6EEvSPkYMnHo7ha2eIUDFEHcTuKg1eSyKvkaPhklg3R5QHl4xo43FnKfQ8TrhAEH07FNKGFVS67xr00a17OD8VNn3LlZISr-iVbaueNBeYD9obUEmL5IJR8Y37qNK4egirn41BXQKK7xguF2nebQpN-1lcewW5OnEWy7yGd7M88l-WVzfNyFCM75bKZFAbv_W2w1glh38M2DJNRbe2SJhMxkpMxwUeLBA\""
			+ "}").getAsJsonObject();

		badResponse = JsonParser.parseString("{"
				+ "\"access_token\":"
				+ "\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
				+ "eyJzdWIiOiJhZG1pbiIsImF6cCI6ImNsaWVudCIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU5NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6Ijc3MmJiODBhLWQ2ZTEtNGUwMi1hN2M3LTAxNWMwZGQyMjdkNSJ9."
				+ "OsuhXSzoODk6VdUjPSyq9vp_ZLqgNczSF1Gvz8-wh1mGdN1aadWy2faKyqIXoQ7OrOycNkOigmyhYoGYoqM30SmkSm9bmpoTOxCAQDclPRdTjmV_-cF9bRLTvaz-78xOcotk4c9kJQHPvUdXiistvnUywME80rKr5K4m4oAYrv-Ii6ntXK6Wbatx3wjEE8Erkq1dkBO9FKCjqIpVeTM-106uFD6yw4sRZZOgY9ibzovp9522l3VkjaU88UEjTu7WbSaLLyaEgiBVSDoweTogEzI-BIZhjtkaxiDzgKvXUJFF8jxyU-2YB4wgs5oi6SUkWJk825I3Lk2v4EZjE3pZag\","
				+ "\"token_type\":\"Bearer\","
				+ "\"scope\":\"address phone openid email profile\","
				+ "\"id_token\":\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
				+ "eyJzdWIiOiI5MDM0Mi5BU0RGSldGQSIsImF1ZCI6ImNsaWVudCIsImF1dGhfdGltZSI6MTUwNjQ1NDIyNywia2lkIjoicnNhMSIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU2NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6IjM2YzU4M2M5LTE1M2UtNDBhOC05M2MzLWEzNWZkYTgwM2QzOCJ9."
				+ "SuGDMrdIh_tCsoWt51xs7rs036KAL3OcwdTcJxmuEUT24EufZILQ9_2rNX8BLA9S-YwYkS243oFz9UfBmnnqj6H27-BO7yBSwGnofDwV6GN4yLXmJfrzC6EEvSPkYMnHo7ha2eIUDFEHcTuKg1eSyKvkaPhklg3R5QHl4xo43FnKfQ8TrhAEH07FNKGFVS67xr00a17OD8VNn3LlZISr-iVbaueNBeYD9obUEmL5IJR8Y37qNK4egirn41BXQKK7xguF2nebQpN-1lcewW5OnEWy7yGd7M88l-WVzfNyFCM75bKZFAbv_W2w1glh38M2DJNRbe2SJhMxkpMxwUeLBA\""
				+ "}").getAsJsonObject();

	}

	/**
	 * Test method for {@link ExtractExpiresInFromTokenEndpointResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_response", goodResponse);

		cond.execute(env);

		assertThat(env.getObject("expires_in")).isNotNull();
	}

	/**
	 * Test method for {@link ExtractExpiresInFromTokenEndpointResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("token_endpoint_response", badResponse);

			cond.execute(env);

		});

	}


}
