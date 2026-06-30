package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ConnectIdCibaTokenResponseConditions_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		JsonObject idToken = new JsonObject();
		idToken.add("claims", new JsonObject());
		env.putObject("id_token", idToken);
	}

	@Test
	public void testIdTokenContainsTxn() {
		env.getObject("id_token").getAsJsonObject("claims").addProperty("txn", "transaction-id");

		execute(new AustraliaConnectIdEnsureIdTokenContainsTxn());
	}

	@Test
	public void testIdTokenMissingTxnFails() {
		assertThatThrownBy(() -> execute(new AustraliaConnectIdEnsureIdTokenContainsTxn()))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("does not contain txn");
	}

	@Test
	public void testIdTokenEmptyTxnFails() {
		env.getObject("id_token").getAsJsonObject("claims").addProperty("txn", "");

		assertThatThrownBy(() -> execute(new AustraliaConnectIdEnsureIdTokenContainsTxn()))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("empty txn");
	}

	@Test
	public void testIdTokenWithoutAcr() {
		execute(new AustraliaConnectIdEnsureIdTokenDoesNotContainAcr());
	}

	@Test
	public void testIdTokenWithAcrFails() {
		env.getObject("id_token").getAsJsonObject("claims").addProperty("acr", "urn:example:acr");

		assertThatThrownBy(() -> execute(new AustraliaConnectIdEnsureIdTokenDoesNotContainAcr()))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("contains an acr claim");
	}

	@Test
	public void testRemoveTxnClaimRequest() {
		JsonObject idTokenClaims = new JsonObject();
		idTokenClaims.add("txn", new JsonObject());
		idTokenClaims.add("given_name", new JsonObject());

		JsonObject claims = new JsonObject();
		claims.add("id_token", idTokenClaims);

		JsonObject request = new JsonObject();
		request.add("claims", claims);
		env.putObject("authorization_endpoint_request", request);

		execute(new RemoveTxnClaimRequestFromAuthorizationEndpointRequest());

		JsonObject updatedClaims = env.getElementFromObject(
			"authorization_endpoint_request", "claims.id_token").getAsJsonObject();
		assertThat(updatedClaims.has("txn")).isFalse();
		assertThat(updatedClaims.has("given_name")).isTrue();
	}

	private void execute(Condition condition) {
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		condition.execute(env);
	}
}
