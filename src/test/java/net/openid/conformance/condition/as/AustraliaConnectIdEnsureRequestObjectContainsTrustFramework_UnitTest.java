package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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
public class AustraliaConnectIdEnsureRequestObjectContainsTrustFramework_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdEnsureRequestObjectContainsTrustFramework cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdEnsureRequestObjectContainsTrustFramework();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noVerifiedClaims() {
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", new JsonObject());
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_valid() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		JsonObject nestedClaims = new JsonObject();
		JsonObject idToken = new JsonObject();
		JsonObject verifiedClaims = new JsonObject();
		JsonObject verification = new JsonObject();
		JsonObject trustFramework = new JsonObject();
		trustFramework.addProperty("value", "au_connectid");
		verification.add("trust_framework", trustFramework);
		verifiedClaims.add("verification", verification);
		idToken.add("verified_claims", verifiedClaims);
		nestedClaims.add("id_token", idToken);
		claims.add("claims", nestedClaims);
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidValue() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		JsonObject nestedClaims = new JsonObject();
		JsonObject idToken = new JsonObject();
		JsonObject verifiedClaims = new JsonObject();
		JsonObject verification = new JsonObject();
		JsonObject trustFramework = new JsonObject();
		trustFramework.addProperty("value", "wrong_framework");
		verification.add("trust_framework", trustFramework);
		verifiedClaims.add("verification", verification);
		idToken.add("verified_claims", verifiedClaims);
		nestedClaims.add("id_token", idToken);
		claims.add("claims", nestedClaims);
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingTrustFramework() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		JsonObject nestedClaims = new JsonObject();
		JsonObject idToken = new JsonObject();
		JsonObject verifiedClaims = new JsonObject();
		idToken.add("verified_claims", verifiedClaims);
		nestedClaims.add("id_token", idToken);
		claims.add("claims", nestedClaims);
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}
}
