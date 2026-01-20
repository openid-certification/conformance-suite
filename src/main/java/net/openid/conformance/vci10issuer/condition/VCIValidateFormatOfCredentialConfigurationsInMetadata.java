package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class VCIValidateFormatOfCredentialConfigurationsInMetadata extends AbstractCondition {

	public static final Set<String> VCI_1_0_CREDENTIAL_FORMATS = Set.of(
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.1
		"jwt_vc_json",
		"jwt_vc_json-ld",
		"ldp_vc-ld",
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.2
		"mso_mdoc",
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.3.1
		"dc+sd-jwt"
	);

	public static final Set<String> HAIP_CREDENTIAL_FORMATS = Set.of(
		// see: https://openid.github.io/OpenID4VC-HAIP/openid4vc-high-assurance-interoperability-profile-wg-draft.html#section-6
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.2
		"mso_mdoc",
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.3.1
		"dc+sd-jwt"
	);

	protected boolean useHaip;

	public VCIValidateFormatOfCredentialConfigurationsInMetadata() {
		this(false);
	}

	public VCIValidateFormatOfCredentialConfigurationsInMetadata(boolean useHaip) {
		this.useHaip = useHaip;
	}

	@Override
	public Environment evaluate(Environment env) {

		Map<String, String> credentialKeysWithUnsupportedFormat = new LinkedHashMap<>();

		Set<String> formatsToCheck = useHaip ? HAIP_CREDENTIAL_FORMATS : VCI_1_0_CREDENTIAL_FORMATS;

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		JsonObject credentialConfigurationsSupported = metadata.getAsJsonObject("credential_configurations_supported");
		for (String credentialKey : credentialConfigurationsSupported.keySet()) {
			JsonObject credentialConfiguration = credentialConfigurationsSupported.getAsJsonObject(credentialKey);
			String credentialFormat = OIDFJSON.getString(credentialConfiguration.get("format"));
			if (!formatsToCheck.contains(credentialFormat)) {
				credentialKeysWithUnsupportedFormat.put(credentialKey, credentialFormat);
			}
		}

		if (!credentialKeysWithUnsupportedFormat.isEmpty()) {
			throw error("Found credential configurations with unsupported formats",
				args("unsupported_credentials", credentialKeysWithUnsupportedFormat));
		}

		logSuccess("Found only credential configurations with supported formats");

		return env;
	}
}
