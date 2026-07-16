package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AddDynamicRegistrationEndpointsToServerConfiguration_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private AddDynamicRegistrationEndpointsToServerConfiguration condition;

	@BeforeEach
	public void setUp() {
		condition = new AddDynamicRegistrationEndpointsToServerConfiguration();
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		JsonObject server = new JsonObject();
		server.add("mtls_endpoint_aliases", new JsonObject());
		env.putObject("server", server);
		env.putString("base_url", "https://rp.example/test");
		env.putString("base_mtls_url", "https://mtls.rp.example/test");
	}

	@Test
	public void addsNormalAndMtlsRegistrationEndpoints() {
		condition.execute(env);

		assertThat(env.getString("server", "registration_endpoint"))
			.isEqualTo("https://rp.example/test/register");
		assertThat(env.getString("server", "mtls_endpoint_aliases.registration_endpoint"))
			.isEqualTo("https://mtls.rp.example/test/register");
	}
}
