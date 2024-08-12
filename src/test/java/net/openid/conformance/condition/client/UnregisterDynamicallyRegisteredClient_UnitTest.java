package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
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

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.noContent;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class UnregisterDynamicallyRegisteredClient_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private UnregisterDynamicallyRegisteredClient cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) throws Exception {
		hoverfly.simulate(dsl(
			service("good.example.com")
				.delete("/deregister")
				.anyBody()
				.willReturn(noContent()),
			service("bad.example.com")
				.delete("/deregister")
				.anyBody()
				.willReturn(badRequest())));
		hoverfly.resetJournal();

		cond = new UnregisterDynamicallyRegisteredClient();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test for {@link UnregisterDynamicallyRegisteredClient#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noErrors(Hoverfly hoverfly){
		env.putString("client", "registration_access_token", "reg.access.token");
		env.putString("client", "registration_client_uri", "https://good.example.com/deregister");
		cond.execute(env);
		hoverfly.verify(service("good.example.com")
			.delete("/deregister")
			.header("Authorization", "Bearer reg.access.token"));
	}

	/**
	 * Test for {@link UnregisterDynamicallyRegisteredClient#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_badResponse(){
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			client.addProperty("registration_access_token", "reg.access.token");
			client.addProperty("registration_client_uri", "https://bad.example.com/deregister");
			env.putObject("client", client);
			cond.execute(env);
		});
	}


}
