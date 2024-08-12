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

@ExtendWith(MockitoExtension.class)
public class EnsureMatchingFAPIInteractionId_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMatchingFAPIInteractionId cond;

	private final String interactionId = "93bac548-d2de-4546-b106-880a5018460d"; // Example from Open Banking spec

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureMatchingFAPIInteractionId();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putString("fapi_interaction_id", interactionId);

	}

	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", interactionId);
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_noErrorCaseInsensitive() {

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", interactionId.toUpperCase());
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_wrongId() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			headers.addProperty("x-fapi-interaction-id", "incorrect");
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_missingId() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);

		});

	}

}
