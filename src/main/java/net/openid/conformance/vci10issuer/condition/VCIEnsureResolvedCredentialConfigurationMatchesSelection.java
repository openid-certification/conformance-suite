package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.VCI1FinalCredentialFormat;

public class VCIEnsureResolvedCredentialConfigurationMatchesSelection extends AbstractCondition {

	private final VCI1FinalCredentialFormat credentialFormat;

	public VCIEnsureResolvedCredentialConfigurationMatchesSelection(VCI1FinalCredentialFormat credentialFormat) {
		this.credentialFormat = credentialFormat;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resolvedCredentialConfiguration = env.getObject("vci_credential_configuration");

		String format = OIDFJSON.getString(resolvedCredentialConfiguration.get("format"));

		if (!credentialFormat.getCredentialFormat().equals(format)) {
			throw error("Format of the resolved credential configuration does not match the expected format",
				args("expected_format", credentialFormat.getCredentialFormat(), "actual_format", format, "credential_configuration", resolvedCredentialConfiguration));
		}

		log("Format of the resolved credential configuration matches the expected format",
			args("expected_format", credentialFormat.getCredentialFormat(), "actual_format", format, "credential_configuration", resolvedCredentialConfiguration));

		return env;
	}
}
