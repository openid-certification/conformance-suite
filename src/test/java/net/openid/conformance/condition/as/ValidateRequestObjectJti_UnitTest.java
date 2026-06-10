package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateRequestObjectJti_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateRequestObjectJti cond;

	@BeforeEach
	public void setUp() {
		RecentValueHistory.clear();
		cond = new ValidateRequestObjectJti();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setRequestObjectJti(JsonElement jti) {
		JsonObject claims = new JsonObject();
		if (jti != null) {
			claims.add("jti", jti);
		}
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);
	}

	@Test
	public void missingJtiFails() {
		setRequestObjectJti(null);
		env.putString("owner_id", "user-a");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void nonStringJtiFails() {
		setRequestObjectJti(new JsonPrimitive(123));
		env.putString("owner_id", "user-a");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyJtiFails() {
		setRequestObjectJti(new JsonPrimitive(""));
		env.putString("owner_id", "user-a");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void firstUseSucceeds() {
		setRequestObjectJti(new JsonPrimitive("jti-1"));
		env.putString("owner_id", "user-a");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void reuseBySameUserFails() {
		env.putString("owner_id", "user-a");
		setRequestObjectJti(new JsonPrimitive("jti-1"));
		assertDoesNotThrow(() -> cond.execute(env)); // first use records the jti

		setRequestObjectJti(new JsonPrimitive("jti-1")); // same jti again
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void sameJtiByDifferentUserSucceeds() {
		env.putString("owner_id", "user-a");
		setRequestObjectJti(new JsonPrimitive("jti-1"));
		assertDoesNotThrow(() -> cond.execute(env));

		env.putString("owner_id", "user-b");
		setRequestObjectJti(new JsonPrimitive("jti-1"));
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void missingOwnerScopeFails() {
		// No owner_id is a test suite bug (the module always surfaces it in configure()), so fail loudly.
		setRequestObjectJti(new JsonPrimitive("jti-1"));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
