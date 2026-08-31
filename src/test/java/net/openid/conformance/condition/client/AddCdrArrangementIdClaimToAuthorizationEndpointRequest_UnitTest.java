package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AddCdrArrangementIdClaimToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AbstractAddCdrArrangementIdClaimToAuthorizationEndpointRequest createCondition(boolean known) {
		AbstractAddCdrArrangementIdClaimToAuthorizationEndpointRequest cond = known
			? new AddCdrArrangementIdClaimToAuthorizationEndpointRequest()
			: new AddUnknownCdrArrangementIdClaimToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		return cond;
	}

	@Test
	public void testEvaluate_knownArrangementId() {
		env.putString("cdr_arrangement_id", "arrangement-123");
		env.putObject("authorization_endpoint_request", new JsonObject());

		createCondition(true).execute(env);

		assertEquals("arrangement-123",
			OIDFJSON.getString(env.getElementFromObject("authorization_endpoint_request", "claims.cdr_arrangement_id")));
	}

	@Test
	public void testEvaluate_knownArrangementIdMissingFromEnv() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("authorization_endpoint_request", new JsonObject());
			createCondition(true).execute(env);
		});
	}

	@Test
	public void testEvaluate_unknownArrangementId() {
		env.putObject("authorization_endpoint_request", new JsonObject());

		createCondition(false).execute(env);

		assertTrue(OIDFJSON.getString(env.getElementFromObject("authorization_endpoint_request", "claims.cdr_arrangement_id"))
			.startsWith("unknown-arrangement-"));
	}

}
