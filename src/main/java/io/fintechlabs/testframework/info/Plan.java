package io.fintechlabs.testframework.info;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.GsonObjectToBsonDocumentConverter;

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

		Module() {
			// Load constructor
		}

		public Module(String module) {
			this.testModule = module;
			this.instances = Collections.emptyList();
		}

		public String getTestModule() {
			return testModule;
		}

		public List<String> getInstances() {
			return instances;
		}
	}

	Plan() {
		// Load constructor
	}

	public Plan(String id,
			String planName,
			String variant,
			JsonObject config,
			Instant started,
			Map<String, String> owner,
			String description,
			String[] testModules,
			String version,
			String summary,
			String publish) {
		this._id = id;
		this.planName = planName;
		this.variant = variant;
		this.config = org.bson.Document.parse(new Gson().toJson(
				GsonObjectToBsonDocumentConverter.convertFieldsToStructure(config)));
		this.started = started.toString();
		this.owner = owner;
		this.description = description;
		this.modules = Arrays.stream(testModules)
				.map(Module::new)
				.collect(Collectors.toList());
		this.version = version;
		this.summary = summary;
		this.publish = publish;
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
