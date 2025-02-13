package net.openid.conformance.condition.as;

import com.android.identity.appsupport.ui.consent.MdocConsentField;
import com.android.identity.cbor.Cbor;
import com.android.identity.cbor.DiagnosticOption;
import com.android.identity.testapp.TestAppUtils;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

public class CreateMdocCredential extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "session_transcript")
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		byte[] sessionTranscript = Base64.getDecoder().decode(env.getString("session_transcript"));

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
