package net.openid.conformance.condition.common;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateRandomRegistrationClientUri_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private CreateRandomRegistrationClientUri condition;

	@BeforeEach
	public void setUp() {
		condition = new CreateRandomRegistrationClientUri();
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void registrationClientUriUsesMtlsBaseWhenExternalUrlOverrideIsPresent() {
		env.putString("base_mtls_url", "https://mtls.example.com");
		env.putString("external_url_override", "https://external.example.com");

		condition.execute(env);

		assertThat(env.getString("registration_client_uri", "fullUrl"))
			.startsWith("https://mtls.example.com/clienturi/");
	}
}
