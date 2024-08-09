package net.openid.conformance.condition.rs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
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

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilGenerateNewPaymentInitiationResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilGenerateNewPaymentInitiationResponse condition;

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

	@Test
	public void test_that_v4_payloads_work(){
		JsonObject paymentInitiationRequest = new JsonObject();
		JsonObject paymentsv4Payload = JsonParser.parseString(paymentsV4PayloadJson).getAsJsonObject();
		paymentInitiationRequest.add("claims", paymentsv4Payload);

		env.putString("fapi_interaction_id", "fapi_interaction_id");
		env.putString("consent_id", "consent_id");
		env.putObject("payment_initiation_request", paymentInitiationRequest);

		condition.evaluate(env);

		JsonArray data = env.getElementFromObject("payment_initiation_response", "data").getAsJsonArray();
		for (JsonElement dataElement : data) {
			assertThat(OIDFJSON.getString(dataElement.getAsJsonObject().get("endToEndId"))).startsWith("E");
		}
	}
}
