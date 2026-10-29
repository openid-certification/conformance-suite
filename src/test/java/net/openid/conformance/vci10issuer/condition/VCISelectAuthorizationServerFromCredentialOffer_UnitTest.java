package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static net.openid.conformance.vci10issuer.condition.AbstractVCICredentialOfferAuthorizationServerCondition.AUTHORIZATION_CODE_GRANT;
import static net.openid.conformance.vci10issuer.condition.AbstractVCICredentialOfferAuthorizationServerCondition.PRE_AUTHORIZED_CODE_GRANT;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS1;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS2;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.authCodeGrant;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.env;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.preAuthGrant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCISelectAuthorizationServerFromCredentialOffer_UnitTest {

	private static final String TWO_SERVERS = "[\"" + AS1 + "\", \"" + AS2 + "\"]";

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCISelectAuthorizationServerFromCredentialOffer cond(String grantType) {
		VCISelectAuthorizationServerFromCredentialOffer cond = new VCISelectAuthorizationServerFromCredentialOffer(grantType);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return cond;
	}

	/** The authorization servers fetched at configuration time, with the first one selected. */
	private static Environment withFetchedServers(Environment env, String... issuers) {
		for (int i = 0; i < issuers.length; i++) {
			JsonObject metadata = new JsonObject();
			metadata.addProperty("issuer", issuers[i]);
			metadata.addProperty("token_endpoint", issuers[i] + "/token");
			env.putObject("vci", "authorization_servers.server" + i + ".authorization_server_metadata", metadata);
		}
		env.putObject("server", env.getElementFromObject("vci", "authorization_servers.server0.authorization_server_metadata").getAsJsonObject().deepCopy());
		return env;
	}

	@Test
	public void theOfferedAuthorizationServerIsSelected() {
		Environment env = withFetchedServers(env(TWO_SERVERS, authCodeGrant(AS2)), AS1, AS2);

		cond(AUTHORIZATION_CODE_GRANT).execute(env);

		assertThat(env.getString("server", "issuer")).isEqualTo(AS2);
	}

	@Test
	public void thePreAuthorizedCodeGrantsHintIsUsedForThatGrant() {
		Environment env = withFetchedServers(env(TWO_SERVERS, preAuthGrant(AS2)), AS1, AS2);

		cond(PRE_AUTHORIZED_CODE_GRANT).execute(env);

		assertThat(env.getString("server", "issuer")).isEqualTo(AS2);
	}

	@Test
	public void theHintOfAnotherGrantIsIgnored() {
		Environment env = withFetchedServers(env(TWO_SERVERS, authCodeGrant(AS2)), AS1, AS2);

		cond(PRE_AUTHORIZED_CODE_GRANT).execute(env);

		assertThat(env.getString("server", "issuer")).isEqualTo(AS1);
	}

	@Test
	public void aConfiguredAuthorizationServerIsKept() {
		Environment env = withFetchedServers(env(TWO_SERVERS, authCodeGrant(AS2)), AS1, AS2);
		env.putString("config", "vci.authorization_server", AS1);

		cond(AUTHORIZATION_CODE_GRANT).execute(env);

		assertThat(env.getString("server", "issuer")).isEqualTo(AS1);
	}

	@Test
	public void anEmptiedFieldCountsAsNotConfigured() {
		Environment env = withFetchedServers(env(TWO_SERVERS, authCodeGrant(AS2)), AS1, AS2);
		env.putString("config", "vci.authorization_server", "");

		cond(AUTHORIZATION_CODE_GRANT).execute(env);

		assertThat(env.getString("server", "issuer")).isEqualTo(AS2);
	}

	@Test
	public void withASingleAuthorizationServerTheHintIsNotUsed() {
		Environment env = withFetchedServers(env("[\"" + AS1 + "\"]", authCodeGrant(AS2)), AS1);

		cond(AUTHORIZATION_CODE_GRANT).execute(env);

		assertThat(env.getString("server", "issuer")).isEqualTo(AS1);
	}

	@Test
	public void anUnlistedAuthorizationServerStopsTheFlow() {
		Environment env = withFetchedServers(env(TWO_SERVERS, authCodeGrant("https://other.example.com")), AS1, AS2);

		assertThatThrownBy(() -> cond(AUTHORIZATION_CODE_GRANT).execute(env)).isInstanceOf(ConditionError.class);
	}
}
