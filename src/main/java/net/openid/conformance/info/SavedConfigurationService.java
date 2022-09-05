package net.openid.conformance.info;

import com.google.gson.JsonObject;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;

public interface SavedConfigurationService {

	Document getLastConfigForCurrentUser();

	void saveTestConfigurationForCurrentUser(JsonObject config, String testName, VariantSelection variant);

	void savePlanConfigurationForCurrentUser(JsonObject config, String planName, VariantSelection variant);

}
