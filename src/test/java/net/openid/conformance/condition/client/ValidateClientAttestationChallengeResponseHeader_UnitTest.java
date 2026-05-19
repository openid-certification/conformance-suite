package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
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
public class ValidateClientAttestationChallengeResponseHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateClientAttestationChallengeResponseHeader cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateClientAttestationChallengeResponseHeader();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putHeader(String value) {
		JsonObject headers = new JsonObject();
		headers.addProperty("oauth-client-attestation-challenge", value);
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.add("headers", headers);
		env.putObject("endpoint_response", endpointResponse);
	}

	@Test
	public void wellFormedChallengePasses() {
		// 43-char base64url, mirrors what dev.id.cloud.dvv.fi returned
		putHeader("Q-nPnPGQ0EaVKh5_20AEWr2-fERrJ87c3QTQnQh0Gf0");

		cond.execute(env);
	}

	@Test
	public void absentHeaderPasses() {
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.add("headers", new JsonObject());
		env.putObject("endpoint_response", endpointResponse);

		cond.execute(env);
	}

	@Test
	public void multipleInstancesFail() {
		JsonArray multi = new JsonArray();
		multi.add("first-challenge-aaaaaaaaaaaaaa");
		multi.add("second-challenge-bbbbbbbbbbbbb");
		JsonObject headers = new JsonObject();
		headers.add("oauth-client-attestation-challenge", multi);
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.add("headers", headers);
		env.putObject("endpoint_response", endpointResponse);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyChallengeFails() {
		putHeader("");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void disallowedCharactersFail() {
		putHeader("contains spaces and !@# symbols xxxxxx");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void tooShortFails() {
		putHeader("abc");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void tooLongFails() {
		putHeader("a".repeat(513));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void tildeAndDotAreAllowed() {
		putHeader("abcdefghijklmnop.~_-");

		cond.execute(env);
	}
}
