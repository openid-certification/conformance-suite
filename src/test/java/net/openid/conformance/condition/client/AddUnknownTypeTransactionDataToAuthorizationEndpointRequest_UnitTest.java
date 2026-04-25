package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class AddUnknownTypeTransactionDataToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddUnknownTypeTransactionDataToAuthorizationEndpointRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new AddUnknownTypeTransactionDataToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_addsBase64UrlEncodedEntryWithCredentialIdFromDcql() {
		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY,
			JsonParser.parseString("""
				{
				  "credentials": [
				    {"id": "id_card_credential", "format": "dc+sd-jwt", "meta": {"vct_values": ["urn:eu.europa.ec.eudi:pid:1"]}}
				  ]
				}""").getAsJsonObject());

		cond.execute(env);

		JsonArray transactionData = env.getObject("authorization_endpoint_request").getAsJsonArray("transaction_data");
		assertThat(transactionData).hasSize(1);

		String encoded = OIDFJSON.getString(transactionData.get(0));
		JsonObject decoded = JsonParser.parseString(Base64URL.from(encoded).decodeToString()).getAsJsonObject();
		assertThat(OIDFJSON.getString(decoded.get("type")))
			.isEqualTo(AddUnknownTypeTransactionDataToAuthorizationEndpointRequest.UNKNOWN_TYPE);
		JsonArray credentialIds = decoded.getAsJsonArray("credential_ids");
		assertThat(credentialIds).hasSize(1);
		assertThat(OIDFJSON.getString(credentialIds.get(0))).isEqualTo("id_card_credential");
	}

	@Test
	public void testEvaluate_failsWhenDcqlHasNoCredentials() {
		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY,
			JsonParser.parseString("{\"credentials\": []}").getAsJsonObject());

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("non-empty array");
	}
}
