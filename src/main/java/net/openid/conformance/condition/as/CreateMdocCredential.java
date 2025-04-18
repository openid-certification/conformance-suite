package net.openid.conformance.condition.as;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.testapp.TestAppUtils;

import java.util.Base64;
import java.util.Set;

public class CreateMdocCredential extends AbstractCondition {

//	private List<MdocConsentField> createConsentList(MdocCredential mdocCredential) {
//		// A map from namespace into a list of data elements where each pair is the data element name and whether the data element will be retained.
//		Map<String, List<Pair<String, Boolean>>> requestedData = new HashMap<>();
//
//		ApplicationData applicationData = mdocCredential.getDocument().getApplicationData();
//
//		if (! applicationData.keyExists("documentData")) {
//			return new ArrayList<MdocConsentField>();
//		}
//
//		NameSpacedData documentData = applicationData.getNameSpacedData("documentData");
//
//		for (String namespace: documentData.getNameSpaceNames()) {
//			// List of credentials within the current namespace.
//			ArrayList<Pair<String, Boolean>> credentials = new ArrayList<>();
//
//			for (String credential: documentData.getDataElementNames(namespace)) {
//				Pair<String, Boolean> credentialPair = new Pair<>(credential, true);
//				credentials.add(credentialPair);
//			}
//
//			requestedData.put(namespace, credentials);
//		}
//
//		return MdocConsentField.Companion.generateConsentFields(DrivingLicense.INSTANCE.getDocumentType().getMdocDocumentType().getDocType(),
//									requestedData,
//									TestAppUtils.documentTypeRepository,
//									mdocCredential);
//	}

	@Override
	@PreEnvironment(strings = "session_transcript")
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		byte[] sessionTranscript = Base64.getDecoder().decode(env.getString("session_transcript"));

		// Ensure the test document is provisioned.
		TestAppUtils testAppUtils = TestAppUtils.INSTANCE;

		// Create a list of consent fields containded in the credential.
//		List<MdocConsentField> mdocConsentFields = createConsentList(TestAppUtils.mdocCredential);
//		List<MdocConsentField> mdocConsentFields = new List<>;


		testAppUtils.initialise();
		byte[] mdoc = testAppUtils.generateDeviceResponse(sessionTranscript);
		env.putString("credential", Base64URL.encode(mdoc).toString());

		String diagnostics = Cbor.INSTANCE.toDiagnostics(mdoc,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		log("Created mdl presentation",
			args("mdoc_b64", Base64URL.encode(mdoc).toString(),
				"cbor_diagnostic", diagnostics));

		return env;
	}

}
