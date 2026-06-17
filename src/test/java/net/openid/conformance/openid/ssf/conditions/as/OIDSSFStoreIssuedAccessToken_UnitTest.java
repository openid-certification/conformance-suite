package net.openid.conformance.openid.ssf.conditions.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFStoreIssuedAccessToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFStoreIssuedAccessToken createCondition() {
		OIDSSFStoreIssuedAccessToken condition = new OIDSSFStoreIssuedAccessToken();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareEnv(String token, String scope) {
		env.putString("access_token", token);
		env.putString("access_token_expiration", "900");
		if (scope != null) {
			env.putString("scope", scope);
		}
		JsonObject client = new JsonObject();
		client.addProperty("client_id", "ssf-test-client");
		env.putObject("client", client);
		env.putObject("ssf", new JsonObject());
		env.putObject("ssf", "issued_tokens", new JsonObject());
	}

	@Test
	void shouldStoreTokenRecordWithClientScopeAndExpiry() {
		long before = Instant.now().getEpochSecond();
		prepareEnv("abc123", "ssf.read ssf.manage");

		assertDoesNotThrow(() -> createCondition().execute(env));

		JsonObject record = env.getElementFromObject("ssf", "issued_tokens").getAsJsonObject()
			.getAsJsonObject("abc123");
		assertNotNull(record);
		assertEquals("ssf-test-client", OIDFJSON.getString(record.get("client_id")));
		assertEquals("ssf.read ssf.manage", OIDFJSON.getString(record.get("scope")));
		long expiresAt = OIDFJSON.getLong(record.get("expires_at"));
		assertTrue(expiresAt >= before + 900);
	}

	@Test
	void shouldStoreTokenRecordWithoutScopeWhenScopeAbsent() {
		prepareEnv("noScopeToken", null);

		assertDoesNotThrow(() -> createCondition().execute(env));

		JsonObject record = env.getElementFromObject("ssf", "issued_tokens").getAsJsonObject()
			.getAsJsonObject("noScopeToken");
		assertNotNull(record);
		assertEquals("ssf-test-client", OIDFJSON.getString(record.get("client_id")));
		assertTrue(record.get("scope") == null);
	}
}
