package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnSecurityEventTokenSubIdUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnSecurityEventTokenSubIdUnknownMembers createCondition() {
		OIDSSFWarnSecurityEventTokenSubIdUnknownMembers condition = new OIDSSFWarnSecurityEventTokenSubIdUnknownMembers();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		return condition;
	}

	private void prepareSubId(String subIdJson) {
		JsonObject claims = new JsonObject();
		if (subIdJson != null) {
			claims.add("sub_id", JsonParser.parseString(subIdJson));
		}
		JsonObject setToken = new JsonObject();
		setToken.add("claims", claims);
		env.putObject("set_token", setToken);
	}

	@Test
	void passesForCleanSubject() {
		prepareSubId("{\"format\":\"email\",\"email\":\"a@example.com\"}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void flagsMembersNotDescribedByTheFormat() {
		prepareSubId("{\"format\":\"email\",\"email\":\"a@example.com\",\"iss\":\"x\"}");
		ConditionError e = assertThrows(ConditionError.class, () -> createCondition().execute(env));
		assertTrue(e.getMessage().contains("does not describe"), e.getMessage());
	}

	@Test
	void passesWhenSubIdIsAbsent() {
		// presence is checked by OIDSSFValidateSecurityEventTokenSubIdClaim
		prepareSubId(null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}
}
