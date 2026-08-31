package net.openid.conformance.condition.as;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrInteractionIdConditions_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private void addIncomingRequestWithInteractionId(String interactionId) {
		String headers = interactionId == null ? "{}" : "{\"x-fapi-interaction-id\":\"" + interactionId + "\"}";
		env.putObject("incoming_request", JsonParser.parseString("{\"headers\":" + headers + "}").getAsJsonObject());
	}

	private <T extends net.openid.conformance.condition.AbstractCondition> T init(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		return cond;
	}

	@Test
	public void testRecord_present() {
		addIncomingRequestWithInteractionId("par-id-1");
		init(new CdrRecordParRequestInteractionId()).execute(env);
		assertEquals("par-id-1", env.getString("par_request_fapi_interaction_id"));
	}

	@Test
	public void testRecord_absent() {
		addIncomingRequestWithInteractionId(null);
		init(new CdrRecordParRequestInteractionId()).execute(env);
		assertNull(env.getString("par_request_fapi_interaction_id"));
	}

	@Test
	public void testMatch_reused() {
		env.putString("par_request_fapi_interaction_id", "id-1");
		addIncomingRequestWithInteractionId("id-1");
		init(new CdrEnsureTokenRequestInteractionIdMatchesParRequest()).execute(env);
	}

	@Test
	public void testMatch_noParId() {
		addIncomingRequestWithInteractionId("id-2");
		init(new CdrEnsureTokenRequestInteractionIdMatchesParRequest()).execute(env);
	}

	@Test
	public void testMatch_different() {
		assertThrows(ConditionError.class, () -> {
			env.putString("par_request_fapi_interaction_id", "id-1");
			addIncomingRequestWithInteractionId("id-2");
			init(new CdrEnsureTokenRequestInteractionIdMatchesParRequest()).execute(env);
		});
	}

}
