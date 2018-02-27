var FAPI_UI = {

	logTemplates : {},

	loadHomepageTemplates : function() {
		this.logTemplates.TEST_OPTION = _.template($("#indexTemplate_TestOption").html());
		this.logTemplates.TEST_OPTGROUP = _.template($("#indexTemplate_TestOptGroup").html());
		this.logTemplates.RUNNING_TEST = _.template($("#indexTemplate_RunningTest").html());
		this.logTemplates.OWNER = _.template($("#logDetailTemplate_Owner").html());
	},

	loadLogDetailTemplates: function() {
		this.logTemplates.LOG_START = _.template($("#logDetailTemplate_LogStart").html());
		this.logTemplates.LOG_DETAIL = _.template($("#logDetailTemplate").html());
		this.logTemplates.SOURCE = _.template($("#logDetailTemplate_Source").html());
		this.logTemplates.MESSAGE = _.template($("#logDetailTemplate_Message").html());
		this.logTemplates.REQUIREMENTS = _.template($("#logDetailTemplate_Requirements").html());
		this.logTemplates.UPLOAD = _.template($("#logDetailTemplate_Upload").html());
		this.logTemplates.OWNER = _.template($("#logDetailTemplate_Owner").html());
		this.logTemplates.RESULT = _.template($("#logDetailTemplate_Result").html());
		this.logTemplates.TIME = _.template($("#logDetailTemplate_Time").html());
		this.logTemplates.MORE = _.template($("#logDetailTemplate_More").html());
		this.logTemplates.MORE_BUTTON = _.template($("#logDetailTemplate_MoreButton").html());
		this.logTemplates.EXPORTED = _.template($("#logDetailTemplate_Exported").html());
		this.logTemplates.BROWSER = _.template($("#logDetailTemplate_Browser").html());
		this.logTemplates.HTTP = _.template($("#logDetailTemplate_Http").html());
	},
	
	loadLogListTemplates: function() {
		this.logTemplates.LOG_LISTING = _.template($("#logsListingTemplate").html());
		this.logTemplates.LOG_LISTING_HEADER = _.template($("#logsListingHeader").html());
		this.logTemplates.OWNER = _.template($("#logDetailTemplate_Owner").html());
	},

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload", "testOwner", "testId", "http"],
	testResults : {passed:false, finished:false, success:0, warning:0, failure:0, interrupted:0, review:0, info:0, default:0, total:0},

	availableTests : {},
	
    getUserInfoDiv : function( divToReplace ) {
		if (!('USER_INFO' in this.logTemplates)) {
			this.logTemplates.USER_INFO = _.template($("#userInfoTemplate").html());
		}
		// get the current user info
		return $.getJSON({
			url: '/currentuser',
			context: this
		}).done(function(userInfo) {
            this.currentUser = userInfo;
            $(divToReplace).html(this.logTemplates.USER_INFO({userInfo: userInfo}));
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
			$('#errorMessage').html(_.escape(error.error));
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
	      , final = props.pop(), p 
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
	    	if (obj != undefined && _.isObject(obj) && _.isArray(elements) && elements.length) {
	    		delete(obj[elements[0]]);
	    	}
	    }
	},

	specLinks : {
	    "FAPI-1-" : "https://bitbucket.org/openid/fapi/src/6bb2d42b34e182c6df45459075898a630ebb08b0/Financial_API_WD_001.md?at=master",
	    "FAPI-2-" : "https://bitbucket.org/openid/fapi/src/6bb2d42b34e182c6df45459075898a630ebb08b0/Financial_API_WD_002.md?at=master",
	    "OB-" : "https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default",
	    "OIDCC-" : "https://openid.net/specs/openid-connect-core-1_0.html",
	    "RFC6749-" : "https://tools.ietf.org/html/rfc6749",
	    "RFC6819-" : "https://tools.ietf.org/html/rfc6819",
	    "RFC7231-" : "https://tools.ietf.org/html/rfc7231"
    },

	testJSON : {}

};


