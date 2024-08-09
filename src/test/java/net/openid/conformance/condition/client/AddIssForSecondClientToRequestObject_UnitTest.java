package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
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
public class AddIssForSecondClientToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddIssForSecondClientToRequestObject cond;

	private String clientId = "21541757519";
	private String client2Id = "8920218600959526";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddIssForSecondClientToRequestObject();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject client = new JsonObject();

		client.addProperty("client_id", clientId);

		env.putObject("client", client);

		JsonObject client2 = new JsonObject();
		client2.addProperty("client_id", client2Id);

		env.putObject("client2", client2);
	}

	@Test
	public void testEvaluate_presentIssValueAndNotEqualClientId() {

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		assertThat(env.getObject("request_object_claims").has("iss")).isTrue();

		assertThat(env.getString("request_object_claims", "iss")).isEqualTo(client2Id);

	}

}
