package net.openid.conformance.condition.client;

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
public class EnsureIncomingRequestAcceptHeaderIsApplicationOauthAuthzReqJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureIncomingRequestAcceptHeaderIsApplicationOauthAuthzReqJwt cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureIncomingRequestAcceptHeaderIsApplicationOauthAuthzReqJwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_exactMatch() {
		env.putString("incoming_request", "headers.accept", "application/oauth-authz-req+jwt");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInsensitive() {
		env.putString("incoming_request", "headers.accept", "Application/OAuth-Authz-Req+JWT");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_withQValue() {
		env.putString("incoming_request", "headers.accept", "application/oauth-authz-req+jwt;q=0.9");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_inList() {
		env.putString("incoming_request", "headers.accept", "application/json, application/oauth-authz-req+jwt, */*");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_wildcardOnly() {
		env.putString("incoming_request", "headers.accept", "*/*");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_wrongType() {
		env.putString("incoming_request", "headers.accept", "application/json");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missing() {
		env.putString("incoming_request", "headers.foo", "bar");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
