package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EnsurePoppyAuthorizationRequestContainsS256CodeChallenge_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyAuthorizationRequestContainsS256CodeChallenge cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyAuthorizationRequestContainsS256CodeChallenge();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

	private void run(String json) {
		env.putObject("effective_authorization_endpoint_request", JsonParser.parseString(json).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void s256Passes() {
		run("{\"code_challenge\":\"" + CHALLENGE + "\",\"code_challenge_method\":\"S256\"}");
		assertEquals(CHALLENGE, env.getString("code_challenge"));
		assertEquals("S256", env.getString("code_challenge_method"));
	}

	@Test
	public void missingChallengeFails() {
		assertThrows(ConditionError.class, () -> run("{\"code_challenge_method\":\"S256\"}"));
	}

	@Test
	public void plainFails() {
		assertThrows(ConditionError.class, () -> run("{\"code_challenge\":\"" + CHALLENGE + "\",\"code_challenge_method\":\"plain\"}"));
	}

	@Test
	public void absentMethodFails() {
		assertThrows(ConditionError.class, () -> run("{\"code_challenge\":\"" + CHALLENGE + "\"}"));
	}

	@Test
	public void wrongLengthChallengeFails() {
		assertThrows(ConditionError.class, () -> run("{\"code_challenge\":\"abc\",\"code_challenge_method\":\"S256\"}"));
	}
}
