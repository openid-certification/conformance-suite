package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class BrowserControl implements DataUtils {

	private static final Logger LOG = LoggerFactory.getLogger(BrowserControl.class);

	protected String testId;

	protected List<String> urls = new ArrayList<>();

	protected List<UrlWithMethod> urlsWithMethod = new ArrayList<>();

	protected List<String> visited = new ArrayList<>();

	protected List<UrlWithMethod> visitedUrlsWithMethod = new ArrayList<>();

	protected List<BrowserApiRequest> browserApiRequests = new ArrayList<>();

	protected boolean showQrCodes = false;

	protected Queue<WebRunner> runners = new ConcurrentLinkedQueue<>();

	public BrowserControl(String testId) {
		this.testId = testId;
	}

	/**
	 * Tell the front-end control that a url needs to be visited. If there is a matching
	 * browser configuration element, this will execute automatically. If there is no
	 * matching element, the url is made available for user interaction.
	 *
	 * @param url the url to be visited
	 */
	public void goToUrl(String url) {
		goToUrl(url, null);
	}

	public void goToUrl(String url, String placeholder) {
		goToUrl(url, placeholder, "GET");
	}

	/**
	 * Tell the front-end control that a url needs to be visited. If there is a matching
	 * browser configuration element, this will execute automatically. If there is no
	 * matching element, the url is made available for user interaction.
	 *
	 * @param url         the url to be visited
	 * @param placeholder the placeholder in the log that is expecting the results of
	 *                    the transaction, usually as a screenshot, can be null
	 * @param method	  the HTTP method to be used
	 */
	public abstract void goToUrl(String url, String placeholder, String method);

	/**
	 * Get the properties of any currently running webrunners.
	 *
	 * @return
	 */
	public List<JsonObject> getWebRunners() {
		return runners.stream().map(WebRunner::toJsonObject).toList();
	}

	public boolean runnersActive() {
		return !runners.isEmpty();
	}

	/**
	 * Request a credential using the Browser API
	 * @param request JSON object that will be passed to the browser API
	 * @param submitUrl URL that log-detail.html should send the results of the browser API call back to
	 */
	public void requestCredential(JsonObject request, String submitUrl) {
		browserApiRequests.add(new BrowserApiRequest(request, submitUrl));
	}

	/**
	 * Tell the front end control that a url has been visited by the user externally.
	 *
	 * @param url the url that has been visited
	 */
	public void urlVisited(String url) {
		LOG.info(testId + ": Browser went to: " + url);

		urls.remove(url);
		visited.add(url);

		Optional<UrlWithMethod> urlWithMethod = urlsWithMethod.stream().filter(u -> Objects.equals(url, u.getUrl())).findFirst();
		if (urlWithMethod.isPresent()) {
			urlsWithMethod.remove(urlWithMethod.get());
			visitedUrlsWithMethod.add(urlWithMethod.get());
		}
	}

	/**
	 * Get the list of URLs that require user interaction.
	 *
	 * @return
	 */
	public List<String> getUrls() {
		return urls;
	}

	public List<UrlWithMethod> getUrlsWithMethod() {
		return urlsWithMethod;
	}

	public List<BrowserApiRequest> getBrowserApiRequests() {
		return browserApiRequests;
	}

	public List<UrlWithMethod> getVisitedUrlsWithMethod() {
		return visitedUrlsWithMethod;
	}

	public boolean showQrCodes() {
		return this.showQrCodes;
	}

	public void setShowQrCodes(boolean showQrCodes) {
		this.showQrCodes = showQrCodes;
	}

	/**
	 * Get the list of URLs that have been visited.
	 *
	 * @return
	 */
	public List<String> getVisited() {
		return visited;
	}

	public abstract static class WebRunner implements Callable<String>  {

		protected String url;
		protected JsonArray tasks;
		protected String placeholder;
		protected String method;

		protected String currentTask;
		protected String currentCommand;
		protected String lastException;

		protected WebRunner(String url, JsonArray tasks, String placeholder, String method) {
			this.url = url;
			this.tasks = tasks;
			this.placeholder = placeholder;
			this.method = method;
		}

		public abstract JsonObject toJsonObject();
	}
}
