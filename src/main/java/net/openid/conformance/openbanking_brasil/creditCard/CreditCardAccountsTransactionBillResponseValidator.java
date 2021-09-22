package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;


/**
 * This class corresponds to {@link CreditCardAccountsTransactionResponseValidator}
 * API: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_credit_cards_apis.yaml
 * Api endpoint: /accounts/{creditCardAccountId}/bills/{billId}/transactions
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */
@ApiName("Credit Card Bill Transaction")
public class CreditCardAccountsTransactionBillResponseValidator extends CreditCardAccountsTransactionResponseValidator {}
