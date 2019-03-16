package io.fintechlabs.testframework.info;

import com.google.gson.JsonObject;
import com.mongodb.DBObject;

public interface SavedConfigurationService {

	DBObject getLastConfigForCurrentUser();

	void saveTestConfigurationForCurrentUser(JsonObject config, String testName);

	void savePlanConfigurationForCurrentUser(JsonObject config, String planName);

}
