# Enum items length and field maxLength swagger inconsistancy

## How to receive inconsistency data to a file

### 1. Setup file logger for enum-checker
Copy or rename [_src/test/resources/logback-test-example.xml_](/src/test/resources/logback-test-example.xml) to
__logback-test.xml__ preserving it on a classpath

### 2. Run unit tests
```bach
mvn clean test
```

### 3. Review the result

The result will be in the _enum-checker.log_ in the root of the project

Unique lines can be selected by the following command
```
sort -u enum-checker.log > enum-checker-sorted.txt
```

## Output line format
Pipe separated
```
Validator name | field name | enum item | enum item length | field max length
```

## 2021-08-29 report
```text
Max length inconsistency | AdvancesContractInstallmentsResponseValidator | typeContractRemaining | SEM_PRAZO_REMANESCENTE | 22 | 6
Max length inconsistency | AdvancesContractInstallmentsResponseValidator | typeNumberOfInstalments | SEM_PRAZO_TOTAL | 15 | 6
Max length inconsistency | AdvancesContractResponseValidator | feeChargeType | POR_PARCELA | 11 | 10
Max length inconsistency | ContractInstallmentsResponseValidator | typeContractRemaining | SEM_PRAZO_REMANESCENTE | 22 | 6
Max length inconsistency | ContractInstallmentsResponseValidator | typeNumberOfInstalments | SEM_PRAZO_TOTAL | 15 | 6
Max length inconsistency | ContractResponseValidator | feeChargeType | POR_PARCELA | 11 | 10
Max length inconsistency | FinancingContractResponseValidator | feeChargeType | POR_PARCELA | 11 | 10
Max length inconsistency | InvoiceFinancingAgreementResponseValidator | feeChargeType | POR_PARCELA | 11 | 10
Max length inconsistency | InvoiceFinancingContractInstallmentsResponseValidator | typeContractRemaining | SEM_PRAZO_REMANESCENTE | 22 | 6
Max length inconsistency | InvoiceFinancingContractInstallmentsResponseValidator | typeNumberOfInstalments | SEM_PRAZO_TOTAL | 15 | 6
Max length is indefined | AccountIdentificationResponseValidator | subtype | 0
Max length is indefined | AccountIdentificationResponseValidator | type | 0
Max length is indefined | AccountListValidator | type | 0
Max length is indefined | AccountTransactionsValidator | completedAuthorisedPaymentType | 0
Max length is indefined | AccountTransactionsValidator | partiePersonType | 0
Max length is indefined | AdvancesContractResponseValidator | instalmentPeriodicity | 0
Max length is indefined | AdvancesContractResponseValidator | productSubType | 0
Max length is indefined | AdvancesContractResponseValidator | productType | 0
Max length is indefined | AdvancesResponseValidator | productSubType | 0
Max length is indefined | AdvancesResponseValidator | productType | 0
Max length is indefined | ConsentDetailsIdentifiedByConsentIdValidator | permissions | 0
Max length is indefined | ConsentDetailsIdentifiedByConsentIdValidator | status | 0
Max length is indefined | ContractResponseValidator | instalmentPeriodicity | 0
Max length is indefined | CorporateRelationshipResponseValidator | type | 0
Max length is indefined | CreateNewConsentValidator | permissions | 0
Max length is indefined | CreateNewConsentValidator | status | 0
Max length is indefined | CreditCardAccountsLimitsResponseValidator | consolidationType | 0
Max length is indefined | CreditCardAccountsLimitsResponseValidator | creditLineLimitType | 0
Max length is indefined | CreditCardAccountsLimitsResponseValidator | lineName | 0
Max length is indefined | CreditCardAccountsTransactionResponseValidator | feeType | 0
Max length is indefined | CreditCardAccountsTransactionResponseValidator | lineName | 0
Max length is indefined | CreditCardAccountsTransactionResponseValidator | otherCreditsType | 0
Max length is indefined | CreditCardAccountsTransactionResponseValidator | paymentType | 0
Max length is indefined | CreditCardAccountsTransactionResponseValidator | transactionType | 0
Max length is indefined | CreditCardBillValidator | paymentMode | 0
Max length is indefined | CreditCardBillValidator | type | 0
Max length is indefined | CreditCardBillValidator | valueType | 0
Max length is indefined | FinancingContractResponseValidator | instalmentPeriodicity | 0
Max length is indefined | InvoiceFinancingAgreementResponseValidator | instalmentPeriodicity | 0
Max length is indefined | InvoiceFinancingAgreementResponseValidator | productSubType | 0
Max length is indefined | InvoiceFinancingAgreementResponseValidator | productType | 0
Max length is indefined | InvoiceFinancingContractsResponseValidator | productType | 0
Max length is indefined | LegalEntityIdentificationValidator | countrySubDivision | 0
Max length is indefined | LegalEntityIdentificationValidator | documentType | 0
Max length is indefined | LegalEntityIdentificationValidator | personType | 0
Max length is indefined | LegalEntityIdentificationValidator | type | 0
Max length is indefined | LegalEntityQualificationResponseValidator | frequency | 0
Max length is indefined | NaturalPersonIdentificationResponseValidator | countrySubDivision | 0
Max length is indefined | NaturalPersonIdentificationResponseValidator | maritalStatusCode | 0
Max length is indefined | NaturalPersonIdentificationResponseValidator | sex | 0
Max length is indefined | NaturalPersonIdentificationResponseValidator | type | 0
Max length is indefined | NaturalPersonRelationshipResponseValidator | type | 0
Max length is indefined | NaturalPersonalQualificationResponseValidator | frequency | 0
Max length is indefined | NaturalPersonalQualificationResponseValidator | occupationCode | 0
Max length is indefined | PaymentInitiationPixPaymentsValidator | status | 0
Max length is indefined | ResourcesResponseValidator | status | 0
Max length is indefined | ResourcesResponseValidator | type | 0
Max length is indefined | net.openid.conformance.apis.OverrideJsonResourceTests$1 | type | 0
```






