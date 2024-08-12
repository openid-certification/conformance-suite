package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddEndToEndIdToPaymentRequestEntityClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddEndToEndIdToPaymentRequestEntityClaims condition;

	private String paymentsV3PayloadJson = """
		{
			"data": {
				"localInstrument": "DICT",
				"payment": {
					"amount": "100000.12",
					"currency": "BRL"
				},
				"creditorAccount": {
					"ispb": "12345678",
					"issuer": "1774",
					"number": "1234567890",
					"accountType": "CACC"
				},
				"cnpjInitiator": "60545350000165",
				"remittanceInformation": "Pagamento da nota XPTO035-002.",
				"proxy": "12345678901"
			}
		}
		""";

	private String paymentsV4PayloadJson = """
		{
			"data": [{
				"localInstrument": "DICT",
				"payment": {
					"amount": "100000.12",
					"currency": "BRL"
				},
				"creditorAccount": {
					"ispb": "12345678",
					"issuer": "1774",
					"number": "1234567890",
					"accountType": "CACC"
				},
				"cnpjInitiator": "60545350000165",
				"remittanceInformation": "Pagamento da nota XPTO035-002.",
				"proxy": "12345678901"
			}]
		}
		""";

	@BeforeEach
	public void setUp() throws Exception {
		condition = new AddEndToEndIdToPaymentRequestEntityClaims();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void test_that_v3_payloads_fail(){
		assertThrows(IllegalStateException.class, () -> {
			JsonObject paymentsv4Payload = JsonParser.parseString(paymentsV3PayloadJson).getAsJsonObject();
			env.putObject("resource_request_entity_claims", paymentsv4Payload);

			condition.evaluate(env);
		});
	}

	@Test
	public void test_that_v4_payloads_work(){
		JsonObject paymentsv4Payload = JsonParser.parseString(paymentsV4PayloadJson).getAsJsonObject();
		env.putObject("resource_request_entity_claims", paymentsv4Payload);

		condition.evaluate(env);

		JsonArray data = env.getElementFromObject("resource_request_entity_claims", "data").getAsJsonArray();
		for (JsonElement dataElement : data) {
			assertThat(OIDFJSON.getString(dataElement.getAsJsonObject().get("endToEndId"))).startsWith("E");
		}
	}
}
