package io.fintechlabs.testframework.info;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = DBTestPlanService.COLLECTION)
public class Plan {

	@Id
	private String _id;
	@Indexed
	private String planName;
	private String variant;
	private org.bson.Document config;
	@Indexed
	private String started;
	@Indexed
	private Map<String, String> owner;
	@Indexed
	private String description;
	private List<Module> modules;
	private String version;
	private String summary;
	@Indexed
	private String publish;

	public static class Module {

		private String testModule;
		private List<String> instances;

		public String getTestModule() {
			return testModule;
		}

		public List<String> getInstances() {
			return instances;
		}
	}

	public String getId() {
		return _id;
	}

	public String getPlanName() {
		return planName;
	}

	public String getVariant() {
		return variant;
	}

	public org.bson.Document getConfig() {
		return config;
	}

	public String getStarted() {
		return started;
	}

	public Map<String, String> getOwner() {
		return owner;
	}

	public String getDescription() {
		return description;
	}

	public List<Module> getModules() {
		return modules;
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
}
