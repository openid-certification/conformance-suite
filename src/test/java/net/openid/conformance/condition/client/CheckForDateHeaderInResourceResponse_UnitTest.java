package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CheckForDateHeaderInResourceResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForDateHeaderInResourceResponse cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForDateHeaderInResourceResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckForDateHeaderInResourceResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("date", DateUtils.formatStandardDate(DateUtils.toInstant(new Date())));
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "date");
	}

	@Test
	public void testEvaluate_oldDate() {
		assertThrows(ConditionError.class, () -> {

			// Obviously, don't run this test on 6 Nov 1994 ;)

			JsonObject headers = new JsonObject();
			headers.addProperty("date", "Sun, 06 Nov 1994 08:49:37 GMT"); // Example from RFC 7231
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "date");
		});
	}

	/**
	 * Test method for {@link CheckForDateHeaderInResourceResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidDate() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			headers.addProperty("date", "this is not a date");
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CheckForDateHeaderInResourceResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingDate() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

}
