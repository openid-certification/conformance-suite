package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EnsureResourceResponseReturnedJsonContentType_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureResourceResponseReturnedJsonContentType cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureResourceResponseReturnedJsonContentType();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link EnsureResourceResponseReturnedJsonContentType#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json; charset=UTF-8");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "content-type");
	}

	/**
	 * Test method for {@link EnsureResourceResponseReturnedJsonContentType#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidCharset() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			headers.addProperty("content-type", "application/json; charset=Shift_JIS");
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link EnsureResourceResponseReturnedJsonContentType#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingCharset() {

		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);
	}

	/**
	 * Test method for {@link EnsureResourceResponseReturnedJsonContentType#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidType() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			headers.addProperty("content-type", "text/json; charset=UTF-8");
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link EnsureResourceResponseReturnedJsonContentType#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingContentType() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link EnsureResourceResponseReturnedJsonContentType#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_contentTypeCannotParse() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			headers.addProperty("content-type", "; charset=UTF-8");
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

}
