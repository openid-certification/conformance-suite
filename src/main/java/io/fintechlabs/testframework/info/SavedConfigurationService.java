package io.fintechlabs.testframework.info;

import org.bson.Document;

import com.google.gson.JsonObject;

public interface SavedConfigurationService {

	Document getLastConfigForCurrentUser();

	void saveTestConfigurationForCurrentUser(JsonObject config, String testName);

	void savePlanConfigurationForCurrentUser(JsonObject config, String planName);

}
