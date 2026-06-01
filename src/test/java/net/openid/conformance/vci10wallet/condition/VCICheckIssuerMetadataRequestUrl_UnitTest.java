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
public class VCICheckIssuerMetadataRequestUrl_UnitTest {

	private VCICheckIssuerMetadataRequestUrl cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCICheckIssuerMetadataRequestUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void passesWhenIssuerHasNoPath() {
		setServerIssuer("https://example.com");
		setIncomingRequestUrl("https://example.com/.well-known/openid-credential-issuer");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenIssuerHasPathWithoutTrailingSlash() {
		setServerIssuer("https://example.com/tenant");
		setIncomingRequestUrl("https://example.com/.well-known/openid-credential-issuer/tenant");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenIssuerHasTrailingSlashAndRequestKeepsIt() {
		// OID4VCI 1.0 Final section 12.2.2 specifies that the well-known suffix is
		// inserted between the host and path components of the Credential Issuer
		// Identifier; the path is preserved verbatim, so a trailing "/" on the
		// identifier is also present on the well-known URL.
		setServerIssuer("https://example.com/tenant/");
		setIncomingRequestUrl("https://example.com/.well-known/openid-credential-issuer/tenant/");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenIssuerHasTrailingSlashAndRequestStripsIt() {
		// Stripping the trailing "/" diverges from the verbatim insert specified in
		// section 12.2.2. (See https://github.com/openid/OpenID4VCI/issues/744.)
		setServerIssuer("https://example.com/tenant/");
		setIncomingRequestUrl("https://example.com/.well-known/openid-credential-issuer/tenant");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenIssuerHasNoTrailingSlashAndRequestAddsOne() {
		setServerIssuer("https://example.com/tenant");
		setIncomingRequestUrl("https://example.com/.well-known/openid-credential-issuer/tenant/");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void passesForRealWorldDeepPath() {
		setServerIssuer("https://demo.certification.openid.net/test/a/mymahi-staging-openid4vci-wallet-test/");
		setIncomingRequestUrl("https://demo.certification.openid.net/.well-known/openid-credential-issuer/test/a/mymahi-staging-openid4vci-wallet-test/");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenWellKnownSuffixIsWrong() {
		setServerIssuer("https://example.com/tenant");
		setIncomingRequestUrl("https://example.com/.well-known/oauth-authorization-server/tenant");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenIssuerPathIsAppendedAfterWellKnown() {
		// "Append after issuer" construction is the OIDC Discovery form and is rejected
		// by OID4VCI section 12.2.2, which mandates the insert-between-host-and-path form.
		setServerIssuer("https://example.com/tenant");
		setIncomingRequestUrl("https://example.com/tenant/.well-known/openid-credential-issuer");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenPathDiffers() {
		setServerIssuer("https://example.com/tenant");
		setIncomingRequestUrl("https://example.com/.well-known/openid-credential-issuer/other");
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
