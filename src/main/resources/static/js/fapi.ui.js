var FAPI_UI = {

		logTemplates : {},

		loadScheduleTestPageTemplates : function() {
			const p1 = fetch('templates/testOption.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_OPTION = _.template(data);
				});

			const p2 = fetch('templates/testOptGroup.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_OPTGROUP = _.template(data);
				});

			const p3 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3];

			return Promise.allSettled(promises);
		},

		loadRunningTestPageTemplates : function() {
			const p1 = fetch('templates/runningTest.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.RUNNING_TEST = _.template(data);
				});

			const p2 = fetch('templates/owner.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				});

			const p3 = fetch('templates/statusAndResult.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
				});

			const p4 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3, p4];

			return Promise.allSettled(promises);
		},

		availablePlans : {},

		getUserInfo : function() {
			// get the current user info
			return fetch("/api/currentuser")
				.then((response) => {
					if (! response.ok) {
						// Tag the error with the status so the catch can
						// distinguish expected-401 from real failures.
						const err = new Error("Network response was not OK");
						err.status = response.status;
						throw err;
					}

					return response.json();
				})
				.then((userInfo) => {
					this.currentUser = userInfo;
					// Pages using <cts-navbar> no longer have #userInfoHolder;
					// guard against its absence while still setting currentUser above.
					const elem = document.getElementById('userInfoHolder');
					if (elem && FAPI_UI.logTemplates.USER_INFO) {
						elem.innerHTML = FAPI_UI.logTemplates.USER_INFO({userInfo: userInfo});
					}
				})
				.catch((error) => {
					// 401 is the expected "not logged in" path — stay quiet.
					// Anything else (network error, 5xx) should log so operators
					// can diagnose silent breakage.
					if (error && error.status !== 401) {
						console.warn("[fapi.ui.js getUserInfo] /api/currentuser failed:", error);
					}
				});

		},

		getStatusHelp : function(value) {
			switch (value ? value.toLowerCase() : undefined) {
				case "waiting":
					return "The test is waiting for something to happen. For example, for the authorization server to redirect back to it, for the user to visit a link or for the user to upload an image (see the test description for details). In some cases this means the authorization server did not redirect back to the conformance suite, indicating that the test failed.";
				case "configured":
					return "The test has successfully setup the initial environment. Please read the test description and when ready press the 'START' button to begin the test.";
				case "interrupted":
					return "The test failed to run to completion as a critical element failed. Please see the log, fix the error and run the test again to get a complete set of results.";
				case "finished":
					return "The test has run to completion.";
				case "running":
					return "The test is actively executing. Reload this page to see the latest status.";
				default:
					return "";
			}
		},

		getResultHelp : function(value) {
			switch (value ? value.toLowerCase() : undefined) {
				case "passed":
					return "The test has passed all conditions.";
				case "failed":
					return "The test has failed at least one critical condition. This means an important error has been detected and the system under test cannot be certified.";
				case "warning":
					return "The test has generated some warnings during its execution, see the log for details. Test results with warnings are accepted for certification, but they generally indicate that the software under test is behaving unexpected or not following recommendations, and the tester should check the results to ensure any warnings are expected behaviour of the software being tested.";
				case "review":
					return "The test requires manual review, for example it contains images that need to be manually checked. These images will be checked by the certification team when a certification request is submitted.";
				case "skipped":
					return "The test could not be completed due to configuration or optional features. Please check if the feature being tested is supported, if it is please check the configuration of the test and of the software under test. If the feature being tested is not supported by the software under test then skipped tests do not prevent certification.";
				default:
					return "";

			}
		},

		/**
		 * Takes in a JSON object representing the error from the server and shows an error display
		 */
		showError : function(error) {
			const elem = document.getElementById('errorMessage');

			if (error != null) {
				var msg = error.error || error.code;
				if (/^\d+$/.test(msg)) {
					// Tomcat considers that HTTP status messages should not be sent,
					// so we get unhelpful responses like "HTTP/1.1 404 404". Make it
					// clear that this is an HTTP error code.
					msg = "HTTP Error " + msg;
				}
				if (error.message) {
					msg += " : " + error.message
				}
				var msgHtml = _.escape(msg);
				if (error.error == "Unauthorized") {
					msgHtml += "<br><br>Refresh the page to renew your session";
				}
				elem.innerHTML = msgHtml;
			} else {
				elem.innerHTML = 'Error from server.';
			}

			FAPI_UI.hideBusy(); // only one modal at a time
			document.getElementById('errorModal').show();
		},

		hideError : function() {
			document.getElementById('errorModal')?.hide();
		},

		showBusy : function(label, message) {
			if (!label) {
				label = "Loading...";
			}

			var elem = document.getElementById('loadingModal-title');
			elem.innerHTML = _.escape(label);
			var elem = document.getElementById('loadingMessage');
			elem.innerHTML = _.escape(message);

			FAPI_UI.hideError(); // only one modal at a time

			document.getElementById('loadingModal').show();
		},

		hideBusy : function() {
			document.getElementById('loadingModal')?.hide();
		},

		selectedVariant: undefined

};
