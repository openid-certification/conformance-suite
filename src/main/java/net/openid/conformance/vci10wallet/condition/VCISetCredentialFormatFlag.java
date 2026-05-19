package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Reads the resolved {@code credential_configuration.format} from env and writes a
 * per-format flag key under {@code env.vci.format_<format>}. Lets downstream
 * credential-creation conditions self-gate via {@code skipIfElementMissing("vci",
 * "format_<format>")} so a single ConditionSequence can carry both mso_mdoc and
 * SD-JWT creation steps with only the active format actually firing.
 *
 * <p>Sets one of (with value {@code "yes"}):
 * <ul>
 *   <li>{@code vci.format_mso_mdoc}</li>
 *   <li>{@code vci.format_sd_jwt} (for {@code dc+sd-jwt} or any non-mso_mdoc format)</li>
 * </ul>
 */
public class VCISetCredentialFormatFlag extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci", "credential_configuration"})
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		if (credentialConfiguration == null || !credentialConfiguration.has("format")) {
			log("credential_configuration.format not present, no flag written.");
			return env;
		}
		String format = OIDFJSON.getString(credentialConfiguration.get("format"));
		JsonObject vci = env.getObject("vci");
		if ("mso_mdoc".equals(format)) {
			vci.addProperty("format_mso_mdoc", "yes");
		} else {
			vci.addProperty("format_sd_jwt", "yes");
		}
		logSuccess("Credential format flag set.", args("format", format));
		return env;
	}
}
