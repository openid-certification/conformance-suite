package io.fintechlabs.testframework.info;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import io.fintechlabs.testframework.testmodule.TestModule.Status;

@Document(collection = DBTestInfoService.COLLECTION)
public class TestInfo {

	@Id
	private String _id;
	private String testId;
	@Indexed
	private String testName;
	private String variant;
	@Indexed
	private String started;
	private org.bson.Document config;
	@Indexed
	private String description;
	private String alias;
	@Indexed
	private Map<String, String> owner;
	private String planId;
	private Status status;
	private String version;
	private String summary;
	@Indexed
	private String publish;
	private String result;

	public String getId() {
		return _id;
	}

	public String getTestId() {
		return testId;
	}

	public String getTestName() {
		return testName;
	}

	public String getVariant() {
		return variant;
	}

	public String getStarted() {
		return started;
	}

	public org.bson.Document getConfig() {
		return config;
	}

	public String getDescription() {
		return description;
	}

	public String getAlias() {
		return alias;
	}

	public Map<String, String> getOwner() {
		return owner;
	}

	public String getPlanId() {
		return planId;
	}

	public Status getStatus() {
		return status;
	}

	public String getVersion() {
		return version;
	}

	public String getSummary() {
		return summary;
	}

	public String getPublish() {
		return publish;
	}

	public String getResult() {
		return result;
	}
}
