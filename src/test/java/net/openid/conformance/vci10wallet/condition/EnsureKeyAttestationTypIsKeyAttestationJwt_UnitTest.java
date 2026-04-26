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
public class EnsureKeyAttestationTypIsKeyAttestationJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureKeyAttestationTypIsKeyAttestationJwt cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureKeyAttestationTypIsKeyAttestationJwt();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putHeaderWithTyp(String typ) {
		JsonObject header = new JsonObject();
		if (typ != null) {
			header.addProperty("typ", typ);
		}
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("header", header);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	@Test
	public void passesWhenTypIsKeyAttestationJwt() {
		putHeaderWithTyp("key-attestation+jwt");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenTypIsWrong() {
		putHeaderWithTyp("openid4vci-proof+jwt");
		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void failsWhenTypIsMissing() {
		putHeaderWithTyp(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}
}
