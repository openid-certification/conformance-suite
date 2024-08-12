package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
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
public class OIDCCValidateClientRedirectUris_UnitTest
{

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDCCValidateClientRedirectUris cond;



	@BeforeEach
	public void setUp() throws Exception {

		cond = new OIDCCValidateClientRedirectUris();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonArray jsonArray = new JsonArray();
		jsonArray.add("https://openid-client.local/cb");
		jsonArray.add("https://client.example.org/callback");
		JsonObject dynRegRequest = new JsonObject();
		dynRegRequest.add("redirect_uris", jsonArray);
		env.putObject("dynamic_registration_request", dynRegRequest);
		env.mapKey("client", "dynamic_registration_request");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_emptyArray() {
		assertThrows(ConditionError.class, () -> {

			JsonArray jsonArray = new JsonArray();
			JsonObject dynRegRequest = new JsonObject();
			dynRegRequest.add("redirect_uris", jsonArray);
			env.putObject("dynamic_registration_request", dynRegRequest);
			env.mapKey("client", "dynamic_registration_request");
			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_string() {
		assertThrows(ConditionError.class, () -> {

			JsonObject dynRegRequest = new JsonObject();
			dynRegRequest.addProperty("redirect_uris", "https://client.example.org/callback");
			env.putObject("dynamic_registration_request", dynRegRequest);
			env.mapKey("client", "dynamic_registration_request");
			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidUri() {
		assertThrows(ConditionError.class, () -> {

			JsonArray jsonArray = new JsonArray();
			jsonArray.add("https://openid-client.local/cb");
			jsonArray.add("invalid uri 910!");
			JsonObject dynRegRequest = new JsonObject();
			dynRegRequest.add("redirect_uris", jsonArray);
			env.putObject("dynamic_registration_request", dynRegRequest);
			env.mapKey("client", "dynamic_registration_request");
			cond.execute(env);

		});

	}
}
