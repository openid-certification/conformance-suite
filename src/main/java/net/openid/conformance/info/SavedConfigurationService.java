package net.openid.conformance.info;

import com.google.gson.JsonObject;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;

public interface SavedConfigurationService {

	Document getLastConfigForCurrentUser();

	void saveTestConfigurationForCurrentUser(JsonObject config, String testName, VariantSelection variant);

	void savePlanConfigurationForCurrentUser(JsonObject config, String planName, VariantSelection variant);

	/**
	 * Hand every saved configuration owned by the given legacy (issuer, subject)
	 * over to the currently authenticated user, so /api/lastconfig keeps finding
	 * the user's saved test configuration after their first IdP login.
	 *
	 * @return the number of saved configurations whose ownership changed
	 */
	long migrateOwnership(String oldIss, String oldSub);
}
