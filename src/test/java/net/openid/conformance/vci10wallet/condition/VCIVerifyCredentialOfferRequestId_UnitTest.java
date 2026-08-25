package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIVerifyCredentialOfferRequestId_UnitTest {

	private VCIVerifyCredentialOfferRequestId cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIVerifyCredentialOfferRequestId();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	private void incomingRequestFor(String url) {
		JsonObject req = new JsonObject();
		req.addProperty("request_url", url);
		env.putObject("incoming_request", req);
	}

	@Test
	public void matchingIdSetsSentinel() {
		env.putString("vci", "credential_offer_id", "abc123");
		incomingRequestFor("https://issuer.example/test/a/x/credential_offer/abc123");

		cond.execute(env);

		assertEquals("abc123", env.getString("vci", "credential_offer_id_matched"));
	}

	@Test
	public void mismatchedIdThrowsAndLeavesNoSentinel() {
		env.putString("vci", "credential_offer_id", "abc123");
		incomingRequestFor("https://issuer.example/test/a/x/credential_offer/other");

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertNull(env.getString("vci", "credential_offer_id_matched"));
	}

	@Test
	public void staleSentinelFromEarlierRequestIsCleared() {
		env.putString("vci", "credential_offer_id", "abc123");
		env.putString("vci", "credential_offer_id_matched", "abc123");
		incomingRequestFor("https://issuer.example/test/a/x/credential_offer/other");

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertNull(env.getString("vci", "credential_offer_id_matched"));
	}

	@Test
	public void missingOfferThrows() {
		incomingRequestFor("https://issuer.example/test/a/x/credential_offer/abc123");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
