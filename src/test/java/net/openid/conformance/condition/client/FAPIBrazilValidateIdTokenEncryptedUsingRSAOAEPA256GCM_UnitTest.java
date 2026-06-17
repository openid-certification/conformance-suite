package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void succeedsForRequiredAlgorithmAndEncryptionMethod() {
		env.putString("id_token", "jwe_header.alg", "RSA-OAEP");
		env.putString("id_token", "jwe_header.enc", "A256GCM");

		cond.execute(env);
	}

	@Test
	public void failsWhenIdTokenIsNotEncrypted() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("id_token", new JsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void failsForWrongAlgorithm() {
		assertThrows(ConditionError.class, () -> {
			env.putString("id_token", "jwe_header.alg", "RSA-OAEP-256");
			env.putString("id_token", "jwe_header.enc", "A256GCM");

			cond.execute(env);
		});
	}

	@Test
	public void failsForWrongEncryptionMethod() {
		assertThrows(ConditionError.class, () -> {
			env.putString("id_token", "jwe_header.alg", "RSA-OAEP");
			env.putString("id_token", "jwe_header.enc", "A128CBC-HS256");

			cond.execute(env);
		});
	}

}
