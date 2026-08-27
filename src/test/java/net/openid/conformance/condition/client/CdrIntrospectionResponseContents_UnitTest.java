package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrIntrospectionResponseContents_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private void addIntrospectionResponse(String bodyJson) {
		env.putObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY,
			JsonParser.parseString("{\"body_json\":" + bodyJson + "}").getAsJsonObject());
	}

	private void addTokenResponse(String bodyJson) {
		env.putObject("token_endpoint_response", JsonParser.parseString(bodyJson).getAsJsonObject());
	}

	private <T extends net.openid.conformance.condition.AbstractCondition> T init(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		return cond;
	}

	@Test
	public void testScope_present() {
		addIntrospectionResponse("{\"active\":true,\"scope\":\"openid accounts\"}");
		init(new CdrEnsureIntrospectionResponseContainsScope()).execute(env);
	}

	@Test
	public void testScope_missing() {
		assertThrows(ConditionError.class, () -> {
			addIntrospectionResponse("{\"active\":true}");
			init(new CdrEnsureIntrospectionResponseContainsScope()).execute(env);
		});
	}

	@Test
	public void testArrangementId_matches() {
		addIntrospectionResponse("{\"active\":true,\"cdr_arrangement_id\":\"arr-1\"}");
		addTokenResponse("{\"access_token\":\"at\",\"cdr_arrangement_id\":\"arr-1\"}");
		init(new CdrEnsureIntrospectionResponseArrangementIdMatchesTokenResponse()).execute(env);
	}

	@Test
	public void testArrangementId_missingFromIntrospection() {
		assertThrows(ConditionError.class, () -> {
			addIntrospectionResponse("{\"active\":true}");
			addTokenResponse("{\"access_token\":\"at\",\"cdr_arrangement_id\":\"arr-1\"}");
			init(new CdrEnsureIntrospectionResponseArrangementIdMatchesTokenResponse()).execute(env);
		});
	}

	@Test
	public void testArrangementId_mismatch() {
		assertThrows(ConditionError.class, () -> {
			addIntrospectionResponse("{\"active\":true,\"cdr_arrangement_id\":\"arr-2\"}");
			addTokenResponse("{\"access_token\":\"at\",\"cdr_arrangement_id\":\"arr-1\"}");
			init(new CdrEnsureIntrospectionResponseArrangementIdMatchesTokenResponse()).execute(env);
		});
	}

	@Test
	public void testArrangementId_notInTokenResponse() {
		addIntrospectionResponse("{\"active\":true,\"cdr_arrangement_id\":\"arr-1\"}");
		addTokenResponse("{\"access_token\":\"at\"}");
		init(new CdrEnsureIntrospectionResponseArrangementIdMatchesTokenResponse()).execute(env);
	}

	@Test
	public void testUsername_absent() {
		addIntrospectionResponse("{\"active\":true,\"scope\":\"openid accounts\"}");
		init(new CdrEnsureIntrospectionResponseDoesNotContainUsername()).execute(env);
	}

	@Test
	public void testUsername_present() {
		assertThrows(ConditionError.class, () -> {
			addIntrospectionResponse("{\"active\":true,\"username\":\"jane\"}");
			init(new CdrEnsureIntrospectionResponseDoesNotContainUsername()).execute(env);
		});
	}

	@Test
	public void testUnexpectedClaims_onlyKnownClaims() {
		addIntrospectionResponse("{\"active\":true,\"exp\":1311281970,\"scope\":\"openid accounts\",\"cdr_arrangement_id\":\"arr-1\"}");
		init(new CdrCheckForUnexpectedClaimsInIntrospectionResponse()).execute(env);
	}

	@Test
	public void testUnexpectedClaims_misspeltClaim() {
		assertThrows(ConditionError.class, () -> {
			addIntrospectionResponse("{\"active\":true,\"exp\":1311281970,\"cdr_arrangment_id\":\"arr-1\"}");
			init(new CdrCheckForUnexpectedClaimsInIntrospectionResponse()).execute(env);
		});
	}

}
