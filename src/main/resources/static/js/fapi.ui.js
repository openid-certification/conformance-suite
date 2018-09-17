var FAPI_UI = {

		logTemplates : {},

		loadHomepageTemplates : function() {
			return $.when(
					$.get('templates/testOption.html', function(data) {
						FAPI_UI.logTemplates.TEST_OPTION = _.template(data);
					}),
					$.get('templates/testOptGroup.html', function(data) {
						FAPI_UI.logTemplates.TEST_OPTGROUP = _.template(data);
					}),
					$.get('templates/runningTest.html', function(data) {
						FAPI_UI.logTemplates.RUNNING_TEST = _.template(data);
					}),
					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					}),
					$.get('templates/statusAndResult.html', function(data) {
						FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
					}),
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					})
			);
		},

		loadLogDetailTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),
					
					$.get('templates/logHeader.html', function(data) {
						FAPI_UI.logTemplates.LOG_START = _.template(data);
					}),

					$.get('templates/logEntry.html', function(data) {
						FAPI_UI.logTemplates.LOG_DETAIL = _.template(data);
					}),

					$.get('templates/source.html', function(data) {
						FAPI_UI.logTemplates.SOURCE = _.template(data);
					}),

					$.get('templates/message.html', function(data) {
						FAPI_UI.logTemplates.MESSAGE = _.template(data);
					}),

					$.get('templates/requirements.html', function(data) {
						FAPI_UI.logTemplates.REQUIREMENTS = _.template(data);
					}),

					$.get('templates/upload.html', function(data) {
						FAPI_UI.logTemplates.UPLOAD = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					}),

					$.get('templates/result.html', function(data) {
						FAPI_UI.logTemplates.RESULT = _.template(data);
					}),

					$.get('templates/time.html', function(data) {
						FAPI_UI.logTemplates.TIME = _.template(data);
					}),

					$.get('templates/more.html', function(data) {
						FAPI_UI.logTemplates.MORE = _.template(data);
					}),

					$.get('templates/moreButton.html', function(data) {
						FAPI_UI.logTemplates.MORE_BUTTON = _.template(data);
					}),

					$.get('templates/exported.html', function(data) {
						FAPI_UI.logTemplates.EXPORTED = _.template(data);
					}),

					$.get('templates/browser.html', function(data) {
						FAPI_UI.logTemplates.BROWSER = _.template(data);
					}),

					$.get('templates/http.html', function(data) {
						FAPI_UI.logTemplates.HTTP = _.template(data);
					}),

					$.get('templates/finalError.html', function(data) {
						FAPI_UI.logTemplates.FINAL_ERROR = _.template(data);
					}),

					$.get('templates/statusAndResult.html', function(data) {
						FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
					}),

					$.get('templates/resultsSummary.html', function(data) {
						FAPI_UI.logTemplates.SUMMARY = _.template(data);
					}),
					
					$.get('templates/startBlock.html', function(data) {
						FAPI_UI.logTemplates.START_BLOCK = _.template(data);
					})
			);

		},

		loadLogListTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),
					
					$.get('templates/logsListEntry.html', function(data) {
						FAPI_UI.logTemplates.LOG_LISTING = _.template(data);
					}),

					$.get('templates/logsListHeader.html', function(data) {
						FAPI_UI.logTemplates.LOG_LISTING_HEADER = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					})
			);

		},

		loadPlanTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),
					
					$.get('templates/plan.html', function(data) {
						FAPI_UI.logTemplates.PLAN_START = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					}),

					$.get('templates/statusAndResult.html', function(data) {
						FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
					})
			);

		},

		loadPlanListTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),
					
					$.get('templates/plansListEntry.html', function(data) {
						FAPI_UI.logTemplates.PLAN_LISTING = _.template(data);
					}),

					$.get('templates/plansListHeader.html', function(data) {
						FAPI_UI.logTemplates.PLAN_LISTING_HEADER = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					})
			);
		},

		loadImageUploadTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),
					
					$.get('templates/pendingImageUploader.html', function(data) {
						FAPI_UI.logTemplates.PENDING = _.template(data);
					}),

					$.get('templates/existingImage.html', function(data) {
						FAPI_UI.logTemplates.EXISTING = _.template(data);
					}),

					$.get('templates/source.html', function(data) {
						FAPI_UI.logTemplates.SOURCE = _.template(data);
					}),

					$.get('templates/message.html', function(data) {
						FAPI_UI.logTemplates.MESSAGE = _.template(data);
					})
			);

		},

		visibleFields : ["msg", "src", "time", "result", "requirements", "upload", "testOwner", "testId", "http", "blockId", "startBlock"],

		availableTests : {},

		availablePlans : {},

		running: false,

		status: 'unknown',

		latestTestEntry: undefined,

		reloadPause: 100,

		maxReloadPause: 5000, // cap at ~5s

		resetReloadPause : function() {
			FAPI_UI.reloadPause = 100; // start at 100ms on reset
		},

		incrementReloadPause : function() {
			if (FAPI_UI.reloadPause < FAPI_UI.maxReloadPause) {
				FAPI_UI.reloadPause += Math.floor(FAPI_UI.reloadPause / 4); // increment by 25%
			}
		},

		getUserInfo : function() {
			// get the current user info
			return $.getJSON({
				url: '/currentuser',
				context: this
			}).done(function(userInfo) {
				this.currentUser = userInfo;
				$('#userInfoHolder').html(FAPI_UI.logTemplates.USER_INFO({userInfo: userInfo}));
				$('[data-toggle="tooltip"]').tooltip();
			});
		},

		getStatusHelp : function(value) {
			switch (value ? value.toLowerCase() : undefined) {
				case "waiting":
					return "The test is waiting for an external callback, for example, for the authorisation server to redirect back to it. In some cases this means the authorisation server did not redirect back to the conformance suite, indicating that the test failed.";
				case "configured":
					return "The test has successfully setup the initial environment. Press the 'START' button to begin the test.";
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
					return "The test has passed all conditions";
				case "failed":
					return "The test has failed at least one critical condition";
				case "warning":
					return "The test has generated some warnings during its execution, see the log for details";
				case "review":
					return "The test requires manual review";
				default:
					return "";

			}
		},

		/**
		 * Takes in a JSON object representing the error from the server and shows an error display
		 */
		showError : function(error) {
			if (error != null) {
				var msg = error.error;
				if (error.message) {
					msg += " : " + error.message
				}
				$('#errorMessage').html(_.escape(msg));
			} else {
				$('#errorMessage').html('Error from server.');
			}

			FAPI_UI.hideBusy(); // only one modal at a time
			$('#errorModal').modal('show');
		},

		hideError : function() {
			$('#errorModal').modal('hide');
		},

		showBusy : function(label, message) {
			if (!label) {
				label = "Loading...";
			}

			$('#loadingLabel').html(_.escape(label));
			$('#loadingMessage').html(_.escape(message));

			FAPI_UI.hideError(); // only one modal at a time
			$('#loadingModal').modal('show');
		},

		hideBusy : function() {
			$('#loadingModal').modal('hide');
		},

		// responsible for converting any dot syntax in our key parameter into object refs
		prop : function(obj, prop, val){
			var props = prop.split('.')
			, final = props.pop(), p;
			while(p = props.shift()){
				if (typeof obj[p] === 'undefined') {
					obj[p] = {}; // create the object
				}

				obj = obj[p];
			}

			return val ? (obj[final] = val) : obj[final];
		},

		removeFromObject : function(obj, key) {
			var elements = key.split('.');
			if (elements.length > 1) {
				this.removeFromObject(obj[elements[0]], elements.splice(1).join('.'));
			} else {
				if (obj !== undefined && _.isObject(obj) && _.isArray(elements) && elements.length) {
					delete(obj[elements[0]]);
				}
			}
		},

		specLinks : {
			"FAPI-R-" : "https://bitbucket.org/openid/fapi/src/6bb2d42b34e182c6df45459075898a630ebb08b0/Financial_API_WD_001.md?at=master",
			"FAPI-RW-" : "https://bitbucket.org/openid/fapi/src/6bb2d42b34e182c6df45459075898a630ebb08b0/Financial_API_WD_002.md?at=master",
			"OB-" : "https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default",
			"OIDCC-" : "https://openid.net/specs/openid-connect-core-1_0.html",
			"RFC6749-" : "https://tools.ietf.org/html/rfc6749",
			"RFC6819-" : "https://tools.ietf.org/html/rfc6819",
			"RFC7231-" : "https://tools.ietf.org/html/rfc7231",
			"HEART-OAuth2-" : "http://openid.net/specs/openid-heart-oauth2-1_0-2017-05-31.html"
		},

		testJSON : {}

};
