package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilGenerateGetPaymentConsentResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPIBrazilGenerateGetPaymentConsentResponse condition;

	@BeforeEach
	public void setUp() {
		condition = new FAPIBrazilGenerateGetPaymentConsentResponse();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject consentResponse = JsonParser.parseString("""
			{
				"data": {
					"consentId": "urn:conformance:oidf:abcdefghij",
					"status": "AUTHORISED"
				},
				"links": {
					"self": "https://mtls.example.com/test-mtls/a/alias/open-banking/payments/v5/consents"
				},
				"meta": {
					"requestDateTime": "2026-01-01T00:00:00Z"
				},
				"aud": "aud",
				"iss": "iss"
			}
			""").getAsJsonObject();
		env.putObject("consent_response", consentResponse);
		env.putString("requested_consent_id", "urn:conformance:oidf:abcdefghij");
		env.putString("fapi_interaction_id", "fapi_interaction_id");
		env.putString("base_mtls_url", "https://mtls.example.com/test-mtls/a/alias");
	}

	@Test
	public void testEvaluate_describesTheGetRequest() {
		condition.execute(env);

		assertThat(env.getString("get_consent_response", "data.status")).isEqualTo("AUTHORISED");
		assertThat(env.getString("get_consent_response", "links.self"))
			.isEqualTo("https://mtls.example.com/test-mtls/a/alias/open-banking/payments/v5/consents/urn:conformance:oidf:abcdefghij");
		assertThat(env.getObject("get_consent_response").getAsJsonObject("meta").keySet()).containsExactly("requestDateTime");
		assertThat(env.getString("get_consent_response_headers", "x-v")).isEqualTo("5.0.0");
		assertThat(env.getString("consent_response", "links.self"))
			.isEqualTo("https://mtls.example.com/test-mtls/a/alias/open-banking/payments/v5/consents");
	}
}
