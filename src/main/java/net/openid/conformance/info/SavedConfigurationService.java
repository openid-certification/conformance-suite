package net.openid.conformance.info;

import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;

import com.google.gson.JsonObject;

public interface SavedConfigurationService {

	Document getLastConfigForCurrentUser();

	void saveTestConfigurationForCurrentUser(JsonObject config, String testName, VariantSelection variant);

	void savePlanConfigurationForCurrentUser(JsonObject config, String planName, VariantSelection variant);

}
