package net.openid.conformance.vp1finalverifier;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.as.CreateRevokedStatusListReference;
import net.openid.conformance.condition.as.VP1FinalGenerateCwtStatusListToken;
import net.openid.conformance.condition.as.VP1FinalGenerateJwtStatusListToken;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Base64;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-present-revoked-credential",
	displayName = "OID4VP-1.0-FINAL Verifier: Present a revoked credential",
	summary = """
		Presents a credential that references a Token Status List, served by this test, that marks \
		the credential as revoked. The verifier must reject the presentation.

		For the SD-JWT VC credential format the credential carries a 'status.status_list' claim and \
		the status list is served as a Status List Token in JWT format, signed with the same key \
		(and certificate chain) as the credential itself. For the ISO mDL credential format the MSO \
		carries the status element of ISO/IEC 18013-5 12.3.6.2 and the status list is served as a \
		Status List Token in CWT format as 12.3.6.3 requires, signed by a certificate issued by the \
		same IACA root as the mdoc's document signer certificate - so a verifier configured with the \
		trust anchors the other tests in this plan need can verify the status list as well.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use \
		the authorization endpoint url below instead of 'openid4vp://' and then start the flow in \
		your verifier as normal.

		On a 4xx response the test passes immediately; on a success response a screenshot of the \
		verifier's error must be uploaded and the test finishes as REVIEW. If the verifier does not \
		fetch the status list at all, it cannot have detected the revocation.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
public class VP1FinalVerifierPresentRevokedCredential extends AbstractVP1FinalVerifierNegativeTest {

	@Override
	protected void createCredential() {
		// must run before the credential is created; the credential carries the reference
		callAndStopOnFailure(CreateRevokedStatusListReference.class, "OTSL-6.2", "ISO18013-5-12.3.6.2");
		super.createCredential();
	}

	/**
	 * Generates the status list token before the authorization response is sent, so that it is
	 * ready to serve however quickly the verifier fetches it — see {@link #handleHttp}.
	 */
	@Override
	protected void customizeAuthorizationEndpointResponseParams() {
		switch (getVariant(VP1FinalVerifierCredentialFormat.class)) {
			case SD_JWT_VC ->
				callAndStopOnFailure(VP1FinalGenerateJwtStatusListToken.class, "OTSL-5.1");
			case ISO_MDL ->
				callAndStopOnFailure(VP1FinalGenerateCwtStatusListToken.class, "OTSL-5.2",
					"ISO18013-5-12.3.6.3");
		}
	}

	/**
	 * Serves the status list without going through the normal request handling.
	 *
	 * <p>{@link AbstractVP1FinalVerifierTest#handleHttp} moves the test to RUNNING, which takes
	 * the test lock and holds it for the whole of the authorization endpoint handler — including
	 * the POST of the authorization response to the verifier's response_uri. A verifier that
	 * checks the status list before it responds to that POST would therefore deadlock against
	 * itself: its status list request would sit behind the lock until the lock acquisition times
	 * out. The token is generated before the response is sent and stored in the environment, so
	 * this handler only has to read it, which needs no lock (the environment is backed by a
	 * concurrent map).
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse servletResponse,
			HttpSession session, JsonObject requestParts) {
		if (CreateRevokedStatusListReference.STATUS_LIST_PATH.equals(path)) {
			return handleStatusListRequest();
		}
		return super.handleHttp(path, req, servletResponse, session, requestParts);
	}

	private Object handleStatusListRequest() {
		boolean isMdoc = getVariant(VP1FinalVerifierCredentialFormat.class)
			== VP1FinalVerifierCredentialFormat.ISO_MDL;
		String contentType = isMdoc
			? VP1FinalGenerateCwtStatusListToken.STATUS_LIST_CWT_CONTENT_TYPE
			: VP1FinalGenerateJwtStatusListToken.STATUS_LIST_JWT_CONTENT_TYPE;
		String token = env.getString(isMdoc
			? VP1FinalGenerateCwtStatusListToken.ENV_KEY : VP1FinalGenerateJwtStatusListToken.ENV_KEY);

		if (token == null) {
			eventLog.log(getName(), "The verifier requested the status list before the presentation "
				+ "was sent, so there is no status list token to serve yet.");
			return ResponseEntity.notFound().build();
		}

		eventLog.log(getName(), "The verifier fetched the status list the presented credential "
			+ "references; it marks that credential as revoked.");

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_TYPE, contentType)
			.body(isMdoc ? Base64.getDecoder().decode(token) : token);
	}
}
