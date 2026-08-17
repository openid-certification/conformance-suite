package net.openid.conformance.vci10issuer.condition;

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
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIExtractDpopNonceFromNonceEndpointResponse_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIExtractDpopNonceFromNonceEndpointResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIExtractDpopNonceFromNonceEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putNonceEndpointResponse(JsonObject headers) {
		JsonObject response = new JsonObject();
		response.addProperty("body", "{\"c_nonce\":\"71893d36023b4895a7c1f93db92d7974\"}");
		response.add("headers", headers);
		env.putObject("endpoint_response", response);
	}

	@Test
	public void storesDpopNonceHeaderForCredentialEndpoint() {
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json");
		headers.addProperty("dpop-nonce", "0a76d7a944ca41d88528b3233146b9c4");
		putNonceEndpointResponse(headers);

		assertDoesNotThrow(() -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isEqualTo("0a76d7a944ca41d88528b3233146b9c4");
	}

	@Test
	public void replacesPreviouslyStoredNonce() {
		env.putString("resource_server_dpop_nonce", "old-nonce");
		JsonObject headers = new JsonObject();
		headers.addProperty("dpop-nonce", "new-nonce");
		putNonceEndpointResponse(headers);

		assertDoesNotThrow(() -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isEqualTo("new-nonce");
	}

	@Test
	public void noHeaderLeavesStoredNonceUntouched() {
		env.putString("resource_server_dpop_nonce", "old-nonce");
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json");
		putNonceEndpointResponse(headers);

		assertDoesNotThrow(() -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isEqualTo("old-nonce");
	}

	@Test
	public void noHeaderAndNothingStoredIsFine() {
		putNonceEndpointResponse(new JsonObject());

		assertDoesNotThrow(() -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isNull();
	}

	@Test
	public void emptyHeaderIsAViolation() {
		JsonObject headers = new JsonObject();
		headers.addProperty("dpop-nonce", "");
		putNonceEndpointResponse(headers);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isNull();
	}

	@Test
	public void headerWithDisallowedCharactersIsAViolation() {
		JsonObject headers = new JsonObject();
		headers.addProperty("dpop-nonce", "nonce with spaces");
		putNonceEndpointResponse(headers);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isNull();
	}

	@Test
	public void multipleHeadersIsAViolation() {
		JsonArray values = new JsonArray();
		values.add("nonce-one");
		values.add("nonce-two");
		JsonObject headers = new JsonObject();
		headers.add("dpop-nonce", values);
		putNonceEndpointResponse(headers);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isNull();
	}
}
