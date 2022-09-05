package net.openid.conformance.info;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.logging.GsonObjectToBsonDocumentConverter;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.VariantSelection;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = DBTestInfoService.COLLECTION)
public class TestInfo {

	@Id
	private String _id;
	private String testId;
	@Indexed
	private String testName;
	private VariantSelection variant;
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

	TestInfo() {
		// Load constructor
	}

	public TestInfo(String id,
			String testName,
			VariantSelection variant,
			Instant started,
			JsonObject config,
			String description,
			String alias,
			Map<String, String> owner,
			String planId,
			String version,
			String summary,
			String publish) {
		this._id = id;
		this.testId = id;
		this.testName = testName;
		this.variant = variant;
		this.started = started.toString();
		this.config = org.bson.Document.parse(new GsonBuilder().serializeNulls().create().toJson(
				GsonObjectToBsonDocumentConverter.convertFieldsToStructure(config)));
		this.description = description;
		this.alias = alias;
		this.owner = owner;
		this.planId = planId;
		this.status = Status.CREATED;
		this.version = version;
		this.summary = summary;
		this.publish = publish;
		this.result = null;
	}

	public String getId() {
		return _id;
	}

	public String getTestId() {
		return testId;
	}

	public String getTestName() {
		return testName;
	}

	public VariantSelection getVariant() {
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
