package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class WarnIfRequestUriMethodInRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private WarnIfRequestUriMethodInRequestObject cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnIfRequestUriMethodInRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noRequestUriMethodPasses() {
		JsonObject claims = new JsonObject();
		claims.addProperty("client_id", "x509_san_dns:example.com");
		claims.addProperty("nonce", "abc123");

		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_requestUriMethodPresentThrowsError() {
		JsonObject claims = new JsonObject();
		claims.addProperty("client_id", "x509_san_dns:example.com");
		claims.addProperty("request_uri_method", "post");

		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
