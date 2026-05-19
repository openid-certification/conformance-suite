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
public class EnsureContentTypeApplicationOauthAuthzReqJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureContentTypeApplicationOauthAuthzReqJwt cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureContentTypeApplicationOauthAuthzReqJwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void acceptsMatchingContentType() {
		env.putObject("endpoint_response", responseWithContentType("application/oauth-authz-req+jwt"));
		cond.execute(env);
	}

	@Test
	public void acceptsMatchingContentTypeWithCharset() {
		env.putObject("endpoint_response", responseWithContentType("application/oauth-authz-req+jwt; charset=utf-8"));
		cond.execute(env);
	}

	@Test
	public void rejectsMismatchedContentType() {
		env.putObject("endpoint_response", responseWithContentType("application/jwt"));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void rejectsMissingContentTypeHeader() {
		JsonObject response = new JsonObject();
		response.add("headers", new JsonObject());
		env.putObject("endpoint_response", response);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private JsonObject responseWithContentType(String contentType) {
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", contentType);
		JsonObject response = new JsonObject();
		response.add("headers", headers);
		return response;
	}
}
