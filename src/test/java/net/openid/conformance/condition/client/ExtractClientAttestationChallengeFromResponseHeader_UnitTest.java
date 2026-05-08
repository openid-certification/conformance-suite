package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
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
public class ExtractClientAttestationChallengeFromResponseHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractClientAttestationChallengeFromResponseHeader cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractClientAttestationChallengeFromResponseHeader();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putHeaders(JsonObject headers) {
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.add("headers", headers);
		env.putObject("endpoint_response", endpointResponse);
	}

	@Test
	public void singleStringHeaderIsHarvested() {
		JsonObject headers = new JsonObject();
		headers.addProperty("oauth-client-attestation-challenge", "fresh-challenge-1");
		putHeaders(headers);

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("fresh-challenge-1");
	}

	@Test
	public void missingHeaderIsNoOp() {
		putHeaders(new JsonObject());

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isNull();
	}

	@Test
	public void emptyHeaderIsNoOp() {
		JsonObject headers = new JsonObject();
		headers.addProperty("oauth-client-attestation-challenge", "");
		putHeaders(headers);

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isNull();
	}

	@Test
	public void multipleHeaderInstancesTakeFirst() {
		JsonArray multi = new JsonArray();
		multi.add("first-challenge");
		multi.add("second-challenge");
		JsonObject headers = new JsonObject();
		headers.add("oauth-client-attestation-challenge", multi);
		putHeaders(headers);

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("first-challenge");
	}
}
