package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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

	@Test
	public void logsWhenNonconformantConfiguredValuesAreOverridden() {
		env.putObjectFromJsonString("client", """
			{
				"id_token_encrypted_response_alg": "RSA1_5",
				"id_token_encrypted_response_enc": "A128CBC-HS256"
			}
			""");

		cond.execute(env);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logEntry = ArgumentCaptor.forClass(Map.class);
		verify(eventLog).log(eq("FAPIBrazilSetRequiredIdTokenEncryptionConfig"), logEntry.capture());
		assertThat(logEntry.getValue())
			.containsEntry("msg", "Overrode nonconformant ID Token encryption configuration required by Open Finance Brazil")
			.containsEntry("previous_id_token_encrypted_response_alg", "RSA1_5")
			.containsEntry("previous_id_token_encrypted_response_enc", "A128CBC-HS256")
			.containsEntry("id_token_encrypted_response_alg", "RSA-OAEP")
			.containsEntry("id_token_encrypted_response_enc", "A256GCM");
	}

}
