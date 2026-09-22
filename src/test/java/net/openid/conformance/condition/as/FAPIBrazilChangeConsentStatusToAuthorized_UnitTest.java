package net.openid.conformance.condition.as;

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

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilChangeConsentStatusToAuthorized_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPIBrazilChangeConsentStatusToAuthorized condition;

	@BeforeEach
	public void setUp() {
		condition = new FAPIBrazilChangeConsentStatusToAuthorized();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_paymentsConsentExpiresAnHourAfterAuthorisation() {
		JsonObject consentResponse = JsonParser.parseString("""
			{
				"data": {
					"status": "AWAITING_AUTHORISATION",
					"expirationDateTime": "2021-01-01T00:05:00Z",
					"payment": { "type": "PIX" }
				}
			}
			""").getAsJsonObject();
		env.putObject("consent_response", consentResponse);

		condition.execute(env);

		assertThat(env.getString("consent_response", "data.status")).isEqualTo("AUTHORISED");
		Instant statusUpdate = Instant.parse(env.getString("consent_response", "data.statusUpdateDateTime"));
		Instant expiration = Instant.parse(env.getString("consent_response", "data.expirationDateTime"));
		assertThat(Duration.between(statusUpdate, expiration)).isEqualTo(Duration.ofMinutes(60));
	}

	@Test
	public void testEvaluate_dataConsentKeepsRequestedExpiration() {
		JsonObject consentResponse = JsonParser.parseString("""
			{
				"data": {
					"status": "AWAITING_AUTHORISATION",
					"expirationDateTime": "2031-01-01T00:00:00Z",
					"permissions": [ "ACCOUNTS_READ" ]
				}
			}
			""").getAsJsonObject();
		env.putObject("consent_response", consentResponse);

		condition.execute(env);

		assertThat(env.getString("consent_response", "data.status")).isEqualTo("AUTHORISED");
		assertThat(env.getString("consent_response", "data.expirationDateTime")).isEqualTo("2031-01-01T00:00:00Z");
	}
}
