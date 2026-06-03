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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCICheckOAuthAuthorizationServerMetadataRequestUrl_UnitTest {

	private VCICheckOAuthAuthorizationServerMetadataRequestUrl cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCICheckOAuthAuthorizationServerMetadataRequestUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void passesWhenIssuerHasNoPath() {
		setServerIssuer("https://example.com");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenIssuerIsHostRootWithTrailingSlash() {
		// Per RFC 8414 section 3.1 the terminating "/" is removed before the well-known
		// suffix is inserted, so the metadata path has no trailing slash.
		setServerIssuer("https://example.com/");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenIssuerHasPathWithoutTrailingSlash() {
		setServerIssuer("https://example.com/issuer1");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server/issuer1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenIssuerHasPathWithTrailingSlash() {
		// Regression: the suite always appends "/" to the configured issuer
		// (see GenerateServerConfigurationMTLS), but RFC 8414 section 3.1 requires the
		// terminating "/" to be removed when forming the well-known URL. A spec-compliant
		// wallet therefore requests the path without a trailing slash and must be accepted.
		setServerIssuer("https://example.com/issuer1/");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server/issuer1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesForRealWorldDeepPath() {
		setServerIssuer("https://demo.certification.openid.net/test/a/mymahi-staging-openid4vci-wallet-test/");
		setIncomingRequestUrl("https://demo.certification.openid.net/.well-known/oauth-authorization-server/test/a/mymahi-staging-openid4vci-wallet-test");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenIssuerHasNoTrailingSlashAndRequestAddsOne() {
		// Lenient mode: many wallets keep the trailing "/" of the issuer when constructing
		// the well-known URL even though RFC 8414 section 3.1 says it MUST be removed.
		// Accept that form too.
		setServerIssuer("https://example.com/issuer1");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server/issuer1/");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenIssuerHasTrailingSlashAndRequestKeepsIt() {
		// The CI panva-style wallet sends this form: it appended the well-known suffix to
		// the issuer (verbatim, keeping the trailing slash). Accepted in lenient mode.
		setServerIssuer("https://example.com/issuer1/");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server/issuer1/");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenWellKnownSuffixIsWrong() {
		setServerIssuer("https://example.com/issuer1");
		setIncomingRequestUrl("https://example.com/.well-known/openid-configuration/issuer1");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenIssuerPathIsAppendedAfterWellKnown() {
		// The "append after issuer" form, e.g. "https://example.com/issuer1/.well-known/...",
		// is the construction described by OpenID Connect Discovery, not RFC 8414. The
		// OAuth-AS metadata check must reject it.
		setServerIssuer("https://example.com/issuer1");
		setIncomingRequestUrl("https://example.com/issuer1/.well-known/oauth-authorization-server");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenPathDiffers() {
		setServerIssuer("https://example.com/issuer1");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server/issuer2");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void setServerIssuer(String issuer) {
		JsonObject server = new JsonObject();
		server.addProperty("issuer", issuer);
		env.putObject("server", server);
	}

	private void setIncomingRequestUrl(String requestUrl) {
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.addProperty("request_url", requestUrl);
		env.putObject("incoming_request", incomingRequest);
	}
}
