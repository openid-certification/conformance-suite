package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AddRequestUriMethodPostToRedirectUrl_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddRequestUriMethodPostToRedirectUrl cond;

	@BeforeEach
	public void setUp() {
		cond = new AddRequestUriMethodPostToRedirectUrl();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_addsPostAndKeepsExistingParameters() {
		env.putString("redirect_to_authorization_endpoint",
			"https://wallet.example.com/authorize?client_id=abc&request_uri=https%3A%2F%2Fverifier.example.com%2Frequest");

		cond.execute(env);

		String result = env.getString("redirect_to_authorization_endpoint");
		assertTrue(result.endsWith("&request_uri_method=post"), "request_uri_method=post should be appended, but got: " + result);
		assertTrue(result.contains("client_id=abc"), "existing parameters must be preserved, but got: " + result);
		assertTrue(result.contains("request_uri="), "existing parameters must be preserved, but got: " + result);
	}

	@Test
	public void testEvaluate_doesNotReEncodeAlreadyEncodedUrl() {
		String redirectTo = "openid4vp://authorize?client_id=decentralized_identifier%3Adid%3Aweb%3Alocalhost%253A8443&request_uri=https%3A%2F%2Fverifier.example.com%2Frequest";
		env.putString("redirect_to_authorization_endpoint", redirectTo);

		cond.execute(env);

		assertEquals(redirectTo + "&request_uri_method=post", env.getString("redirect_to_authorization_endpoint"));
	}
}
