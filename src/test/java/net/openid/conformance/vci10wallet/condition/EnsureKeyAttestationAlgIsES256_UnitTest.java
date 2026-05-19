package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureKeyAttestationAlgIsES256_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureKeyAttestationAlgIsES256 cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureKeyAttestationAlgIsES256();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putHeaderWithAlg(String alg) {
		JsonObject header = new JsonObject();
		if (alg != null) {
			header.addProperty("alg", alg);
		}
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("header", header);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	@Test
	public void passesWhenAlgIsES256() {
		putHeaderWithAlg("ES256");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenAlgIsRS256() {
		putHeaderWithAlg("RS256");
		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void failsWhenAlgIsMissing() {
		putHeaderWithAlg(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}
}
