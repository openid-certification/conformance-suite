package net.openid.conformance.condition.client.lote;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;

/**
 * Registers the list of trusted entities (ETSI TS 119 602 LoTE) configured for the slot named by
 * the {@code lote_slot} string: creates the slot's environment object (mapped to {@code "lote"})
 * with the URL to fetch the list from. With no URL configured the object is not created, and the
 * downstream LoTE conditions skip.
 */
public class RegisterLote extends AbstractCondition {

	/** The environment string naming the {@link LoteSlot} being set up. */
	public static final String SLOT = "lote_slot";

	@Override
	@PreEnvironment(required = "config", strings = SLOT)
	public Environment evaluate(Environment env) {
		LoteSlot slot = LoteSlot.valueOf(env.getString(SLOT));

		// the config form keeps cleared fields as empty strings, so treat blank as absent
		String url = Strings.emptyToNull(env.getString("config", slot.configKey));
		if (url == null) {
			env.removeObject(AbstractLoteCondition.LOTE);
			log("No " + slot.fieldDescription() + "; skipping the trust checks against that list of trusted entities");
			return env;
		}
		url = url.trim();
		// a trusted list fetched over plaintext must not be used as the source of trust decisions
		if (!url.toLowerCase(Locale.ROOT).startsWith("https://")) {
			throw error("The " + slot.fieldDescription() + " must be an https:// URL",
				args("url", url));
		}

		JsonObject lote = new JsonObject();
		lote.addProperty("slot", slot.name());
		lote.addProperty("url", url);
		if (slot.expectedLoteType != null) {
			lote.addProperty("expected_type", slot.expectedLoteType);
		}
		env.putObject(AbstractLoteCondition.LOTE, lote);
		logSuccess("Registered the " + slot.fieldDescription() + "; the list will be fetched from it",
			args("url", url, "slot", slot.name()));
		return env;
	}
}
