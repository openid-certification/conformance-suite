package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilSetRequiredIdTokenEncryptionConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPIBrazilSetRequiredIdTokenEncryptionConfig cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPIBrazilSetRequiredIdTokenEncryptionConfig();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void setsOpenFinanceBrazilRequiredIdTokenEncryptionConfig() {
		env.putObject("client", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("client", "id_token_encrypted_response_alg")).isEqualTo("RSA-OAEP");
		assertThat(env.getString("client", "id_token_encrypted_response_enc")).isEqualTo("A256GCM");
	}

}
