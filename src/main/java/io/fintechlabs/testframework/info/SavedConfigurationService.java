package io.fintechlabs.testframework.info;

import org.bson.Document;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.variant.VariantSelection;

public interface SavedConfigurationService {

	Document getLastConfigForCurrentUser();

	void saveTestConfigurationForCurrentUser(JsonObject config, String testName, VariantSelection variant);

	void savePlanConfigurationForCurrentUser(JsonObject config, String planName, VariantSelection variant);

}
