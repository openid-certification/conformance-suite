package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AddCdrSharingDurationClaimToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private long runCondition(AbstractAddCdrSharingDurationClaimToAuthorizationEndpointRequest cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		cond.execute(env);
		return OIDFJSON.getLong(env.getElementFromObject("authorization_endpoint_request", "claims.sharing_duration"));
	}

	@Test
	public void testEvaluate_default90Days() {
		env.putObject("authorization_endpoint_request", new JsonObject());
		assertEquals(7776000, runCondition(new AddCdrSharingDurationClaimToAuthorizationEndpointRequest()));
	}

	@Test
	public void testEvaluate_zero() {
		env.putObject("authorization_endpoint_request", new JsonObject());
		assertEquals(0, runCondition(new AddCdrSharingDurationClaimZeroToAuthorizationEndpointRequest()));
	}

	@Test
	public void testEvaluate_negative() {
		env.putObject("authorization_endpoint_request", new JsonObject());
		assertEquals(-1, runCondition(new AddCdrSharingDurationClaimNegativeToAuthorizationEndpointRequest()));
	}

	@Test
	public void testEvaluate_overwritesExistingValueAndKeepsOtherClaims() {
		env.putObject("authorization_endpoint_request",
			JsonParser.parseString("{\"claims\":{\"sharing_duration\":7776000,\"id_token\":{\"acr\":{\"value\":\"urn:cds.au:cdr:2\"}}}}").getAsJsonObject());
		assertEquals(0, runCondition(new AddCdrSharingDurationClaimZeroToAuthorizationEndpointRequest()));
		assertEquals("urn:cds.au:cdr:2",
			OIDFJSON.getString(env.getElementFromObject("authorization_endpoint_request", "claims.id_token.acr.value")));
	}

}
