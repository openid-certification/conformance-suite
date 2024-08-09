package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureIdTokenDoesNotContainNonRequestedClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIdTokenDoesNotContainNonRequestedClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureIdTokenDoesNotContainNonRequestedClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_claimsFromScope() {
		// Ensure the authorization request exists and contains scopes that map to claims.
		JsonObject scope = JsonParser.parseString("{"
			+ "\"scope\": \"openid email\""
			+ "}")
			.getAsJsonObject();
		env.putObject("authorization_endpoint_request", scope);

		// Add some supported claims to the id token including those mapped from the claims above.
		JsonObject claims = JsonParser.parseString("{"
			+ "\"claims\": {"
				+ "\"sub\": null,"
				+ "\"exp\": null,"
				+ "\"email_verified\": null,"
				+ "\"email\": null"
			+ "}}")
			.getAsJsonObject();
		env.putObject("id_token", claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidClaim() {
		assertThrows(ConditionError.class, () -> {
			// Ensure the authorization request exists and contains scopes that map to claims.
			JsonObject scope = JsonParser.parseString("{"
				+ "\"scope\": \"openid email\""
				+ "}")
				.getAsJsonObject();
			env.putObject("authorization_endpoint_request", scope);

			// Add some supported claims to the id token including those mapped from the claims above + an invalid claim.
			JsonObject claims = JsonParser.parseString("{"
				+ "\"claims\": {"
				+ "\"sub\": null,"
				+ "\"exp\": null,"
				+ "\"email_verified\": null,"
				+ "\"email\": null,"
				+ "\"invalid\": null"
				+ "}}")
				.getAsJsonObject();
			env.putObject("id_token", claims);

			cond.execute(env);
		});
	}
}
