package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FAPIBrazilValidateIdTokenEncryptionConfig_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private FAPIBrazilValidateIdTokenEncryptionConfig condition;

	@BeforeEach
	public void setUp() {
		condition = new FAPIBrazilValidateIdTokenEncryptionConfig();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void acceptsRequiredValuesWithoutChangingTheRegistrationRequest() {
		JsonObject client = createClient("RSA-OAEP", "A256GCM");
		JsonObject submittedMetadata = client.deepCopy();
		env.putObject("client", client);

		assertThatCode(() -> condition.execute(env)).doesNotThrowAnyException();

		assertThat(env.getObject("client")).isEqualTo(submittedMetadata);
	}

	@Test
	public void rejectsMissingEncryptionMetadata() {
		env.putObject("client", new JsonObject());

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("RSA-OAEP")
			.hasMessageContaining("A256GCM");
	}

	@Test
	public void rejectsDifferentKeyEncryptionAlgorithm() {
		env.putObject("client", createClient("RSA-OAEP-256", "A256GCM"));

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("RSA-OAEP")
			.hasMessageContaining("A256GCM");
	}

	@Test
	public void rejectsDifferentContentEncryptionAlgorithm() {
		env.putObject("client", createClient("RSA-OAEP", "A128GCM"));

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("RSA-OAEP")
			.hasMessageContaining("A256GCM");
	}

	private JsonObject createClient(String algorithm, String encryptionMethod) {
		JsonObject client = new JsonObject();
		client.addProperty("id_token_encrypted_response_alg", algorithm);
		client.addProperty("id_token_encrypted_response_enc", encryptionMethod);
		return client;
	}
}
