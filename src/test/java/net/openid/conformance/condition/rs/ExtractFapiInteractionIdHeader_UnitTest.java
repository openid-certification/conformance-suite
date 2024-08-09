package net.openid.conformance.condition.rs;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExtractFapiInteractionIdHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractFapiInteractionIdHeader cond;

	private String id = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a"; // example from FAPI spec
	private String altId = "this-is-an-arbitrary-string";

	private JsonObject goodRequest;
	private JsonObject altRequest;
	private JsonObject missingHeader;
	private JsonObject noHeaders;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractFapiInteractionIdHeader();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-interaction-id\": \"" + id + "\""
			+ "}}").getAsJsonObject();
		altRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-interaction-id\": \"" + altId + "\""
			+ "}}").getAsJsonObject();
		missingHeader = JsonParser.parseString("{\"headers\":{}}").getAsJsonObject();
		noHeaders = JsonParser.parseString("{}").getAsJsonObject();

	}

	@Test
	public void test_good() {

		env.putObject("incoming_request", goodRequest);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-interaction-id");
		assertEquals(id, env.getString("fapi_interaction_id"));
	}

	@Test
	public void test_alt() {

		env.putObject("incoming_request", altRequest);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-interaction-id");
		assertEquals(altId, env.getString("fapi_interaction_id"));
	}

	@Test
	public void test_missing() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", missingHeader);
			cond.execute(env);
		});
	}

	@Test
	public void test_noHeader() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", noHeaders);
			cond.execute(env);
		});
	}

}
