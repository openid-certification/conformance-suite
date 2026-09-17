package net.openid.conformance.condition.rs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
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

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilGenerateNewPaymentInitiationResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPIBrazilGenerateNewPaymentInitiationResponse condition;

	private String paymentsPayloadJson = """
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
				"remittanceInformation": "Pagamento da nota XPTO035-002.",
				"qrCode": "00020104141234567890123426660014BR.GOV.BCB.PIX014466756C616E6F32303139406578616D706C652E636F6D27300012  \\nBR.COM.OUTRO011001234567895204000053039865406123.455802BR5915NOMEDORECEBEDOR6008BRASILIA61087007490062  \\n530515RP12345678-201950300017BR.GOV.BCB.BRCODE01051.0.080450014BR.GOV.BCB.PIX0123PADRAO.URL.PIX/0123AB  \\nCD81390012BR.COM.OUTRO01190123.ABCD.3456.WXYZ6304EB76\\n",
				"proxy": "12345678901",
				"cnpjInitiator": "61820817000109",
				"endToEndId": "E00000000202407311248Nqa8UwJVdye"
			}]
		}
		""";

	@BeforeEach
	public void setUp() throws Exception {
		condition = new FAPIBrazilGenerateNewPaymentInitiationResponse();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setUpPaymentInitiationRequest() {
		JsonObject paymentInitiationRequest = new JsonObject();
		paymentInitiationRequest.add("claims", JsonParser.parseString(paymentsPayloadJson));

		env.putString("fapi_interaction_id", "fapi_interaction_id");
		env.putString("consent_id", "consent_id");
		env.putString("base_mtls_url", "https://mtls.example.com/test-mtls/a/alias");
		env.putObject("payment_initiation_request", paymentInitiationRequest);
	}

	@Test
	public void test_that_payloads_work() {
		setUpPaymentInitiationRequest();

		condition.execute(env);

		JsonArray data = env.getElementFromObject("payment_initiation_response", "data").getAsJsonArray();
		for (JsonElement dataElement : data) {
			JsonObject payment = dataElement.getAsJsonObject();
			assertThat(OIDFJSON.getString(payment.get("endToEndId"))).startsWith("E");
			assertThat(OIDFJSON.getString(payment.get("status"))).isEqualTo("RCVD");
			assertThat(payment.getAsJsonObject("debtorAccount").keySet()).contains("ispb", "number", "accountType");
		}
		assertThat(env.getString("payment_initiation_response", "links.self"))
			.isEqualTo("https://mtls.example.com/test-mtls/a/alias/open-banking/payments/v5/pix/payments");
		assertThat(env.getString("payment_initiation_response_headers", "x-v")).isEqualTo("5.0.0");
	}

	@Test
	public void test_that_debtor_account_is_taken_from_consent() {
		setUpPaymentInitiationRequest();
		JsonObject debtorAccount = JsonParser.parseString("""
			{
				"ispb": "87654321",
				"issuer": "6272",
				"number": "94088392",
				"accountType": "SVGS"
			}
			""").getAsJsonObject();
		JsonObject consentData = new JsonObject();
		consentData.add("debtorAccount", debtorAccount);
		JsonObject consentResponse = new JsonObject();
		consentResponse.add("data", consentData);
		env.putObject("consent_response", consentResponse);

		condition.execute(env);

		JsonArray data = env.getElementFromObject("payment_initiation_response", "data").getAsJsonArray();
		assertThat(data.get(0).getAsJsonObject().get("debtorAccount")).isEqualTo(debtorAccount);
	}
}
