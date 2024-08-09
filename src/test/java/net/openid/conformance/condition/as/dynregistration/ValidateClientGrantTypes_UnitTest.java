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
public class ValidateClientGrantTypes_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateClientGrantTypes cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateClientGrantTypes();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_Success() {
		JsonObject client = new JsonObject();
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("authorization_code");
		grantTypes.add("implicit");
		JsonArray responseTypes = new JsonArray();
		responseTypes.add("code");
		responseTypes.add("code id_token");
		responseTypes.add("id_token");
		responseTypes.add("code token id_token");
		client.add("grant_types", grantTypes);
		client.add("response_types", responseTypes);
		env.putObject("client", client);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_MissingImplicit() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			JsonArray grantTypes = new JsonArray();
			grantTypes.add("authorization_code");
			JsonArray responseTypes = new JsonArray();
			responseTypes.add("code");
			responseTypes.add("code id_token");
			responseTypes.add("id_token");
			responseTypes.add("code token id_token");
			client.add("grant_types", grantTypes);
			client.add("response_types", responseTypes);
			env.putObject("client", client);
			cond.execute(env);
		});
	}

	//grantTypes is set but "empty". it won't default to authorization_code
	@Test
	public void testEvaluate_GrantTypesIsIncludedButEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			JsonArray grantTypes = new JsonArray();
			JsonArray responseTypes = new JsonArray();
			responseTypes.add("code");
			client.add("grant_types", grantTypes);
			client.add("response_types", responseTypes);
			env.putObject("client", client);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_NoGrantTypesAtAll() {
		//client has no grant_types
		JsonObject client = new JsonObject();
		JsonArray responseTypes = new JsonArray();
		responseTypes.add("code");
		client.add("response_types", responseTypes);
		env.putObject("client", client);
		cond.execute(env);
	}
}
