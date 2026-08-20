package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FAPIBrazilAddRequiredIdTokenEncryptionToDynamicRegistrationRequest_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private FAPIBrazilAddRequiredIdTokenEncryptionToDynamicRegistrationRequest condition;

	@BeforeEach
	public void setUp() {
		condition = new FAPIBrazilAddRequiredIdTokenEncryptionToDynamicRegistrationRequest();
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putObject("dynamic_registration_request", new JsonObject());
	}

	@Test
	public void addsRequiredIdTokenEncryptionMetadata() {
		condition.execute(env);

		assertThat(env.getString("dynamic_registration_request", "id_token_encrypted_response_alg"))
			.isEqualTo("RSA-OAEP");
		assertThat(env.getString("dynamic_registration_request", "id_token_encrypted_response_enc"))
			.isEqualTo("A256GCM");
	}
}
