package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The issuer_state of a credential offer is what the next authorization request carries; one
 * left over from an earlier offer in the same test (the multiple-clients module) must not be.
 */
public class VCITryToExtractIssuerStateFromCredentialOffer_UnitTest {

	private VCITryToExtractIssuerStateFromCredentialOffer cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCITryToExtractIssuerStateFromCredentialOffer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void theOffersIssuerStateIsStored() {
		putCredentialOffer("""
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_configuration_ids": ["UniversityDegreeCredential"],
			  "grants": { "authorization_code": { "issuer_state": "state-of-this-offer" } }
			}
			""");

		cond.execute(env);

		assertThat(env.getString("vci", "issuer_state")).isEqualTo("state-of-this-offer");
	}

	@Test
	public void aNewOffersIssuerStateReplacesThePreviousOne() {
		putCredentialOffer("""
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_configuration_ids": ["UniversityDegreeCredential"],
			  "grants": { "authorization_code": { "issuer_state": "state-of-second-offer" } }
			}
			""");
		env.putString("vci", "issuer_state", "state-of-first-offer");

		cond.execute(env);

		assertThat(env.getString("vci", "issuer_state")).isEqualTo("state-of-second-offer");
	}

	@Test
	public void anOfferWithoutIssuerStateDropsThePreviousOne() {
		putCredentialOffer("""
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_configuration_ids": ["UniversityDegreeCredential"],
			  "grants": { "authorization_code": {} }
			}
			""");
		env.putString("vci", "issuer_state", "state-of-first-offer");

		cond.execute(env);

		assertThat(env.getString("vci", "issuer_state")).isNull();
	}

	@Test
	public void anOfferWithoutIssuerStateAndNothingStoredLeavesNothing() {
		putCredentialOffer("""
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_configuration_ids": ["UniversityDegreeCredential"]
			}
			""");

		cond.execute(env);

		assertThat(env.getString("vci", "issuer_state")).isNull();
	}

	private void putCredentialOffer(String json) {
		JsonObject offer = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_offer", offer);
		env.putObject("vci", vci);
	}
}
