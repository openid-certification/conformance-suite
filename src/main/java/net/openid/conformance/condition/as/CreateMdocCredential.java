package net.openid.conformance.condition.as;

import com.android.identity.appsupport.ui.consent.MdocConsentField;
import com.android.identity.cbor.Cbor;
import com.android.identity.cbor.DiagnosticOption;
import com.android.identity.testapp.TestAppUtils;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CreateMdocCredential extends AbstractMdocSessionTranscript {

	@Override
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		byte[] nonceBytes = new byte[16];
		new SecureRandom().nextBytes(nonceBytes);
		String mdocGeneratedNonce = Base64URL.encode(nonceBytes).toString();
		env.putString("mdoc_generated_nonce", mdocGeneratedNonce);

		String clientId = env.getString("client", "client_id");
		String responseUri = env.getString("authorization_request_object", "claims.response_uri");
		String nonce = env.getString("nonce");

		byte[] sessionTranscript = createSessionTranscript(clientId, responseUri, nonce, mdocGeneratedNonce);

		// This will result in an mdoc without any fields (no name/etc). We should perhaps attempt to parse the PE/DCQL.
		List<MdocConsentField> mdocConsentFields = new ArrayList<>();

		byte[] mdoc = TestAppUtils.INSTANCE.generateEncodedDeviceResponse(mdocConsentFields, sessionTranscript);

		env.putString("credential", Base64URL.encode(mdoc).toString());

		String diagnostics = Cbor.INSTANCE.toDiagnostics(mdoc,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		log("Created mdl presentation",
			args("mdoc_b64", Base64URL.encode(mdoc).toString(),
				"cbor_diagnostic", diagnostics));

		return env;
	}

}
