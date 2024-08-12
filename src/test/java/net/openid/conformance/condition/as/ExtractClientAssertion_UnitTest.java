package net.openid.conformance.condition.as;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractClientAssertion_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject tokenEndpointRequest;

	private ExtractClientAssertion cond;

	/**
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractClientAssertion();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		String clientAssertion =  "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6" +
			"ImMxZGYwZTdhLTAzMWMtNDIzMi05MzE0LTg3MTdkOWJjY2RiNCJ9.eyJpYXQiOjE1N" +
			"Dg4NDQwNzUsImV4cCI6MTU0ODg0NDEzNSwianRpIjoiaDFfOVhOd1ZNazBoMy1rSWs" +
			"3YjJzUHB0aF9rU1pwRWVFSW1JSUxGSEZBMCIsImlzcyI6InRlc3QtY2xpZW50LWlkL" +
			"TM0NjMzNGFkZ2RzZmdkZmczNDI1Iiwic3ViIjoidGVzdC1jbGllbnQtaWQtMzQ2MzM" +
			"0YWRnZHNmZ2RmZzM0MjUiLCJhdWQiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzL3Rlc" +
			"3QtbXRscy9hL2ZpbnRlY2gtY2xpZW50dGVzdC90b2tlbiJ9.JrJNOC4-9ziSch4x9U" +
			"6pcdyQIyh7WwILv23Oq2EoLocLnXuJi5GK7JCbzs6doB2X3lS2M481WucI_dpGZcJM" +
			"bioi9anHRJQ5KcyXNHvmcIU0YYHRhkr0eFhRUk_tBeHx0sM0Wr34UovpODN_S6hK2x" +
			"2QyTTCyiEqVTZkLc2GdJyoMOkglkkvAGv67QviQpeZssXnGQk_WHBCPMv2BMVL9iT1" +
			"nnXrdKJB8qABkAlUrnie4177UQ_JMccUR-IpgS0-y3O3ioaJOoANAZdM_A2NTq-JcJ" +
			"8jwmRs8TbdZ-COD-QNFkPUIrOSZLp-IGZR4ULI06bH24yNJBhHhFoMeapR3w";

		JsonObject sampleParams = new JsonObject();
		sampleParams.addProperty("client_assertion", clientAssertion);

		tokenEndpointRequest = new JsonObject();
		tokenEndpointRequest.add("body_form_params", sampleParams);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_request", tokenEndpointRequest);

		cond.execute(env);

		assertThat(env.containsObject("client_assertion")).isTrue();
	}

	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			tokenEndpointRequest.add("body_form_params", new JsonObject());

			env.putObject("token_endpoint_request", tokenEndpointRequest);

			cond.execute(env);

		});

	}

}
