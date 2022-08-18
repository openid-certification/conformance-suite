package net.openid.conformance.openbanking_brasil.plans;

public class PlanNames {
	/** VERSION 1 **/
	/* Phase 1 - Open Data */
	public static final String ADMIN_API_TEST_PLAN  = "Functional tests for Admin API - based on Swagger version: 1.0.1 (WIP)";

	public static final String BANKING_AGENTS_API_TEST_PLAN = "Functional tests for Channels - BankingAgents API - based on Swagger version: 1.0.3 (WIP)";
	public static final String BRANCHES_API_TEST_PLAN = "Functional tests for Channels - Branches API - based on Swagger version: 1.0.3 (WIP)";
	public static final String ELECTRONIC_CHANNELS_API_TEST_PLAN = "Functional tests for Channels - Electronic Channels API - based on Swagger version: 1.0.3 (WIP)";
	public static final String PHONE_CHANNELS_API_TEST_PLAN = "Functional tests for Channels - Phone Channels API - based on Swagger version: 1.0.3 (WIP)";
	public static final String SHARED_AUTOMATED_TELLER_MACHINES_API_TEST_PLAN = "Functional tests for Channels - Shared Automated Teller Machines API - based on Swagger version: 1.0.3 (WIP)";


	public static final String BUSINESS_ACCOUNTS_API_TEST_PLAN = "Functional tests for ProductsNServices - BusinessAccounts API - based on Swagger version: 1.0.0 (WIP)";
	public static final String BUSINESS_CREDIT_CARD_API_TEST_PLAN = "Functional tests for ProductsNServices - BusinessCreditCard API - based on Swagger version: 1.0.0 (WIP)";
	public static final String BUSINESS_FINANCINGS_API_TEST_PLAN = "Functional tests for ProductsNServices - BusinessFinancings API - based on Swagger version: 1.0.0 (WIP)";
	public static final String BUSINESS_INVOICE_FINANCINGS_API_TEST_PLAN = "Functional tests for ProductsNServices - BusinessInvoiceFinancings API - based on Swagger version: 1.0.0 (WIP)";
	public static final String BUSINESS_LOANS_API_TEST_PLAN = "Functional tests for ProductsNServices - BusinessLoans API - based on Swagger version: 1.0.0 (WIP)";
	public static final String PERSONAL_ACCOUNTS_API_TEST_PLAN = "Functional tests for ProductsNServices - PersonalAccounts API - based on Swagger version: 1.0.0 (WIP)";
	public static final String PERSONAL_CREDIT_CARD_API_TEST_PLAN = "Functional tests for ProductsNServices - PersonalCreditCard API - based on Swagger version: 1.0.0 (WIP)";
	public static final String PERSONAL_FINANCINGS_API_TEST_PLAN = "Functional tests for ProductsNServices - PersonalFinancings API - based on Swagger version: 1.0.0 (WIP)";
	public static final String PERSONAL_INVOICE_FINANCINGS_API_TEST_PLAN = "Functional tests for ProductsNServices - PersonalInvoiceFinancings API - based on Swagger version: 1.0.0 (WIP)";
	public static final String PERSONAL_LOANS_API_TEST_PLAN = "Functional tests for ProductsNServices - PersonalLoans API - based on Swagger version: 1.0.0 (WIP)";
	public static final String UNARRANGED_ACCOUNT_BUSINESS_OVERDRAFT_API_TEST_PLAN = "Functional tests for ProductsNServices - UnarrangedAccountBusinessOverdraft API - based on Swagger version: 1.0.0 (WIP)";
	public static final String UNARRANGED_ACCOUNT_PERSONAL_OVERDRAFT_API_TEST_PLAN = "Functional tests for ProductsNServices - UnarrangedAccountPersonalOverdraft API - based on Swagger version: 1.0.0 (WIP)";

	/* Phase 2 - Customer Data */

	public static final String ACCOUNT_API_NAME = "Functional tests for accounts API - based on Swagger version: 1.0.3";
	public static final String CONSENTS_API_NAME = "Functional tests for consents API - based on Swagger version: 1.0.3";

	public static final String CREDIT_CARDS_API_PLAN_NAME = "Functional tests for Credit Card API - based on swagger version: 1.0.4";

	public static final String CUSTOMER_PERSONAL_DATA_API_PLAN_NAME = "Functional tests for personal customer data API - based on Swagger version: 1.0.3";
	public static final String CUSTOMER_BUSINESS_DATA_API_PLAN_NAME = "Functional tests for business customer data API - based on Swagger version: 1.0.3";
	public static final String RESOURCES_API_PLAN_NAME = "Functional tests for resources API - based on Swagger version: 1.0.2";

	public static final String CREDIT_OPERATIONS_ADVANCES_API_PLAN_NAME = "Functional tests for unarranged overdraft API - based on Swagger version: 1.0.4";
	public static final String LOANS_API_PLAN_NAME = "Functional tests for loans API - based on Swagger version: 1.0.4";
	public static final String FINANCINGS_API_NAME = "Functional tests for financings API - based on Swagger version: 1.0.4";

	/* Phase 3 - Payment Initiation */

	public static final String OBB_DCR = "Open Banking Brazil - DCR";
	public static final String OBB_DCR_WITHOUT_BROWSER_INTERACTION_TEST_PLAN = "Brazil DCR Test without Browser Interaction";

	public static final String PAYMENTS_API_PHASE_2_TEST_PLAN = "Functional tests for payments API INIC, DICT, MANU, QRES and QRDN (T0/T1/T2) - Based on Swagger version: 1.0.1";
	public static final String PAYMENTS_API_PHASE_3_TEST_PLAN = "Functional tests for payments PIX Scheduling (T3) - Based on Swagger version: 1.1.0 (WIP)";


	public static final String CREDIT_OPERATIONS_DISCOUNTED_CREDIT_RIGHTS_API_PLAN_NAME = "Functional tests for discounted credit rights API - based on Swagger version: 1.0.4";
	public static final String PAYMENTS_API_PHASE_1_TEST_PLAN = "Functional tests for payments API INIC, DICT and MANU (T0/T1) - Based on Swagger version: 1.0.1 - Limit Submission Date 14/01";

	public static final String PAYMENTS_API_ALL_TEST_PLAN = "Functional tests for payments API INIC, DICT, MANU, QRES, QRDN, Scheduling (T0/T1/T2/T3) - Based on Swagger version: 1.1.0 (WIP)";

	/** VERSION 2 **/
	public static  final String LATEST_VERSION_2 = "2.0.1 (WIP)";

	public static final String ACCOUNT_API_NAME_V2 = "Functional tests for accounts API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String CONSENTS_API_NAME_V2 = "Functional tests for consents API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String CREDIT_CARDS_API_PLAN_NAME_V2 = "Functional tests for Credit Card API - based on swagger version: " + LATEST_VERSION_2;
	public static final String CREDIT_OPERATIONS_DISCOUNTED_CREDIT_RIGHTS_API_PLAN_NAME_V2 = "Functional tests for discounted credit rights API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String CUSTOMER_PERSONAL_DATA_API_PLAN_NAME_V2 = "Functional tests for personal customer data API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String CUSTOMER_BUSINESS_DATA_API_PLAN_NAME_V2 = "Functional tests for business customer data API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String RESOURCES_API_PLAN_NAME_V2 = "Functional tests for resources API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String CREDIT_OPERATIONS_ADVANCES_API_PLAN_NAME_V2 = "Functional tests for unarranged overdraft API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String LOANS_API_PLAN_NAME_V2 = "Functional tests for loans API - based on Swagger version: " + LATEST_VERSION_2;
	public static final String FINANCINGS_API_NAME_V2 = "Functional tests for financings API - based on Swagger version: " + LATEST_VERSION_2;

	public static final String OPERATIONAL_LIMITS_PLAN_NAME_V2 = "Functional tests for operational limits - based on Swagger version: " + LATEST_VERSION_2;



}
