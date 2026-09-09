package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureGrantedScopeIsReadOnly_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureGrantedScopeIsReadOnly createCondition() {
		var condition = new OIDSSFEnsureGrantedScopeIsReadOnly();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareTokenResponse(String scope) {
		JsonObject response = new JsonObject();
		response.addProperty("access_token", "token");
		if (scope != null) {
			response.addProperty("scope", scope);
		}
		env.putObject("token_endpoint_response", response);
	}

	@Test
	void shouldPassWhenOnlyReadScopeGranted() {
		prepareTokenResponse("ssf.read");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenScopeIsOmitted() {
		// RFC 6749 5.1: scope is OPTIONAL "if identical to the scope requested by the client"
		prepareTokenResponse(null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldPassWhenScopeIsEmpty() {
		prepareTokenResponse("");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenManageScopeGrantedAlongsideRead() {
		// RFC 6749 3.3 lets the AS ignore the requested scope; a token that can manage
		// streams cannot demonstrate the transmitter's scope enforcement
		prepareTokenResponse("ssf.read ssf.manage");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void shouldFailWhenOnlyManageScopeGranted() {
		prepareTokenResponse("ssf.manage");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
