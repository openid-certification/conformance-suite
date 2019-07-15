package io.fintechlabs.testframework.info;

import org.bson.Document;

import com.google.gson.JsonObject;

public interface SavedConfigurationService {

	Document getLastConfigForCurrentUser();

	void saveTestConfigurationForCurrentUser(JsonObject config, String testName, String variant);

	void savePlanConfigurationForCurrentUser(JsonObject config, String planName, String variant);

}
