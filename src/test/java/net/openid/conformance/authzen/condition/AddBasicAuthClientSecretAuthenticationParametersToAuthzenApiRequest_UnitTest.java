package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client_secret_basic method: RFC 6749 Section 2.3.1 form-encodes the id and secret before
 * they are joined and base64-encoded.
 */
@ExtendWith(MockitoExtension.class)
class AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	private void fails(AbstractCondition cond) {
		assertThrows(ConditionError.class, () -> run(cond));
	}

	private String decodedCredentials() {
		String header = OIDFJSON.getString(env.getObject("authzen_api_endpoint_request_headers").get("Authorization"));
		assertTrue(header.startsWith("Basic "), header);
		return new String(java.util.Base64.getDecoder().decode(header.substring("Basic ".length())), java.nio.charset.StandardCharsets.UTF_8);
	}

	@Test
	public void plainCredentials_areSentAsIs() {
		env.putObject("client", json("{ \"client_id\": \"pep-1\", \"client_secret\": \"s3cret_value\" }"));
		run(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
		assertEquals("pep-1:s3cret_value", decodedCredentials());
	}

	@Test
	public void specialCharacters_areFormEncoded() {
		env.putObject("client", json("{ \"client_id\": \"urn:pep:1\", \"client_secret\": \"a+b/c= d\" }"));
		run(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
		assertEquals("urn%3Apep%3A1:a%2Bb%2Fc%3D+d", decodedCredentials());
	}

	@Test
	public void existingHeaders_areKept() {
		env.putObject("client", json("{ \"client_id\": \"pep-1\", \"client_secret\": \"x\" }"));
		env.putObject("authzen_api_endpoint_request_headers", json("{ \"X-Request-ID\": \"req-1\" }"));
		run(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
		assertEquals("req-1", OIDFJSON.getString(env.getObject("authzen_api_endpoint_request_headers").get("X-Request-ID")));
	}

	@Test
	public void missingSecret_fails() {
		env.putObject("client", json("{ \"client_id\": \"pep-1\" }"));
		fails(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
	}

	@Test
	public void missingId_fails() {
		env.putObject("client", json("{ \"client_secret\": \"x\" }"));
		fails(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
	}

	@Test
	public void nullId_fails() {
		env.putObject("client", json("{ \"client_id\": null, \"client_secret\": \"x\" }"));
		fails(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
	}

	@Test
	public void emptyId_failsWithoutAddingAHeader() {
		env.putObject("client", json("{ \"client_id\": \"\", \"client_secret\": \"x\" }"));
		ConditionError e = assertThrows(ConditionError.class, () -> run(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest()));
		assertTrue(e.getMessage().contains("'client_id' field in the 'Client' section of the test configuration is empty"), e.getMessage());
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}

	@Test
	public void whitespaceOnlyId_fails() {
		env.putObject("client", json("{ \"client_id\": \" \\t \", \"client_secret\": \"x\" }"));
		fails(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
	}

	@Test
	public void nullSecret_fails() {
		env.putObject("client", json("{ \"client_id\": \"pep-1\", \"client_secret\": null }"));
		fails(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
	}

	@Test
	public void emptySecret_failsWithoutAddingAHeader() {
		env.putObject("client", json("{ \"client_id\": \"pep-1\", \"client_secret\": \"\" }"));
		ConditionError e = assertThrows(ConditionError.class, () -> run(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest()));
		assertTrue(e.getMessage().contains("'client_secret' field in the 'Client' section of the test configuration is empty"), e.getMessage());
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}

	@Test
	public void whitespaceOnlySecret_fails() {
		env.putObject("client", json("{ \"client_id\": \"pep-1\", \"client_secret\": \"  \" }"));
		fails(new AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest());
	}
}
