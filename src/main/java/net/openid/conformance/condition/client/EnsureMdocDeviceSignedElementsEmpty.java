package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Asserts that the mdoc's DeviceSigned structure contains no data elements.
 *
 * ISO/IEC 18013-5 does allow an issuer to authorize data elements to be returned in
 * DeviceNameSpaces (via KeyAuthorizations in the MSO), so a wallet returning device-signed
 * elements is not necessarily non-conformant. However the suite currently only processes
 * issuerSigned elements when matching the returned claims against the DCQL query; before
 * accepting claims from DeviceSigned it would need to verify the MSO's KeyAuthorizations
 * covers them (which DeviceResponseParser does not do). Until that is implemented, flag any
 * device-signed elements loudly rather than silently ignoring them.
 */
public class EnsureMdocDeviceSignedElementsEmpty extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"mdoc"})
	public Environment evaluate(Environment env) {

		JsonElement deviceSignedElements = env.getElementFromObject("mdoc", "device_signed_elements");
		if (deviceSignedElements == null) {
			throw error("mdoc object does not contain device_signed_elements");
		}

		if (!deviceSignedElements.getAsJsonObject().isEmpty()) {
			throw error("mdoc DeviceSigned structure contains data elements. The conformance suite only checks issuerSigned "
					+ "elements against the DCQL query; it does not currently support claims returned in deviceSigned "
					+ "(that would require verifying the elements are authorized by KeyAuthorizations in the MSO). "
					+ "If your wallet legitimately returns issuer-authorized device-signed elements, please raise an issue "
					+ "at https://gitlab.com/openid/conformance-suite/-/issues",
				args("device_signed_elements", deviceSignedElements));
		}

		logSuccess("mdoc DeviceSigned structure contains no data elements");
		return env;
	}
}
