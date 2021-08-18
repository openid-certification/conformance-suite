package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ReorderGrantTypesInDynamicRegistrationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;


	private ReorderGrantTypesInDynamicRegistrationRequest cond;

	private JsonObject dynamicRegistrationRequest;

	@Before
	public void setUp() throws Exception {
		cond = new ReorderGrantTypesInDynamicRegistrationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		dynamicRegistrationRequest = new JsonObject();
		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);
	}

	@Test
	public void testEvaluate_good(){
		JsonArray grants = new JsonArray();
		grants.add("a");
		grants.add("b");
		grants.add("c");
		dynamicRegistrationRequest.add("grant_types", grants);
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request")).isNotNull();
		assertThat(env.getElementFromObject("dynamic_registration_request","grant_types").toString()).isEqualTo("[\"c\",\"b\",\"a\"]");
	}

	@Test(expected = NullPointerException.class)
	public void testEvaluate_noGrantTypes(){
		cond.execute(env);
	}

}
