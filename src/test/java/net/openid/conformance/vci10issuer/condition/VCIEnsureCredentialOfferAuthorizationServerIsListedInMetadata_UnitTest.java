package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS1;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS2;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.ISSUER;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.authCodeGrant;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.env;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.preAuthGrant;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCIEnsureCredentialOfferAuthorizationServerIsListedInMetadata_UnitTest {

	private VCIEnsureCredentialOfferAuthorizationServerIsListedInMetadata cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialOfferAuthorizationServerIsListedInMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	@Test
	public void noAuthorizationServerInTheOfferPasses() {
		Environment env = env("[\"" + AS1 + "\"]", "{\"authorization_code\": {\"issuer_state\": \"s\"}}");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aListedAuthorizationServerPasses() {
		Environment env = env("[\"" + AS1 + "\", \"" + AS2 + "\"]", authCodeGrant(AS2));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void anUnlistedAuthorizationServerFails() {
		Environment env = env("[\"" + AS1 + "\", \"" + AS2 + "\"]", authCodeGrant("https://other.example.com"));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void anUnlistedAuthorizationServerInThePreAuthorizedCodeGrantFails() {
		Environment env = env("[\"" + AS1 + "\", \"" + AS2 + "\"]", preAuthGrant("https://other.example.com"));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void withoutAuthorizationServersTheCredentialIssuerMatches() {
		Environment env = env(null, authCodeGrant(ISSUER));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void withoutAuthorizationServersAnotherServerFails() {
		// the Launchpad case in issue #2062: an authorization_server the metadata does not know of
		Environment env = env(null, authCodeGrant(AS1));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}
}
