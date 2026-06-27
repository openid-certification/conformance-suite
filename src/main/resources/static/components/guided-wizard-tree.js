/**
 * Decision tree for Guided Test Plan Creation.
 *
 * Converted once from MR !2029's `guided-wizard.yaml` (Domingos) into a
 * JSDoc-typed ES module so the structure is validated by the frontend
 * type-check gate instead of a runtime YAML parser (CDN scripts are banned
 * on this branch and no YAML mechanism exists in the frontend). Content and
 * comments are carried over from the YAML; if the YAML evolves upstream,
 * this module must be updated by hand — the colocated integrity test
 * (`guided-wizard-tree.test.js`) catches structural breakage.
 *
 * One deliberate data fix relative to the YAML: `dcr_brazil_op`'s
 * `also_required` pointed at a nonexistent `fapi2_brazil_op` choice; it now
 * points at `fapi1_brazil_op` ("FAPI Security Profile"), mirroring the
 * existing `fapi1_brazil_op → dcr_brazil_op` reference.
 *
 * From the YAML header:
 * - Each choice has either `next` (another question) or `result` (a leaf).
 * - `result.plan_name` must match a testPlanName from `@PublishTestPlan`.
 * - `result.variants` keys must match variant parameter names (see
 *   `variant/*.java` enums).
 * - `result.also_required` lists sibling choice ids needed for full
 *   certification.
 */

/**
 * @typedef {object} AlsoRequired
 * @property {string} id - Sibling choice id within the same step's `choices`.
 * @property {string} label - Plan label shown in the bundle checklist.
 */

/**
 * @typedef {object} WizardResult
 * @property {string} plan_name - testPlanName from `@PublishTestPlan`.
 * @property {Record<string, string>} variants - Variant parameter name → value.
 * @property {AlsoRequired[]} [also_required] - Sibling plans needed for full
 *   certification.
 */

/**
 * @typedef {object} WizardChoice
 * @property {string} id
 * @property {string} label
 * @property {string} [description] - Optional supporting copy under the label.
 * @property {WizardStep} [next] - Follow-up question (exactly one of
 *   `next`/`result` per choice — enforced by the integrity test).
 * @property {WizardResult} [result] - Leaf resolution.
 */

/**
 * @typedef {object} WizardStep
 * @property {string} id
 * @property {string} question
 * @property {WizardChoice[]} choices
 */

/**
 * @typedef {object} WizardEcosystem
 * @property {string} id
 * @property {string} label
 * @property {WizardStep[]} steps - The journey enters at `steps[0]`.
 */

/**
 * @typedef {object} WizardTree
 * @property {WizardEcosystem[]} ecosystems
 */

/** @type {WizardTree} */
export const GUIDED_WIZARD_TREE = {
  ecosystems: [
    {
      id: "open_finance_brazil",
      label: "🇧🇷 OpenFinance Brazil",
      steps: [
        {
          id: "role",
          question: "What is your role?",
          choices: [
            {
              id: "rp",
              label: "RP (Client)",
              next: {
                id: "scope",
                question: "Which type of client?",
                choices: [
                  {
                    id: "accounts",
                    label: "Just Account access",
                    result: {
                      plan_name: "fapi1-advanced-final-client-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_profile: "openbanking_brazil",
                        fapi_auth_request_method: "pushed",
                        fapi_response_mode: "plain_response",
                        fapi_client_type: "oidc",
                        brazil_client_scope: "openid-accounts",
                      },
                    },
                  },
                  {
                    id: "payments",
                    label: "Payment Initiation client",
                    result: {
                      plan_name: "fapi1-advanced-final-client-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_profile: "openbanking_brazil",
                        fapi_auth_request_method: "pushed",
                        fapi_response_mode: "plain_response",
                        fapi_client_type: "oidc",
                        brazil_client_scope: "openid-payments",
                      },
                    },
                  },
                  {
                    id: "ciba",
                    label: "CIBA client",
                    result: {
                      plan_name: "fapi-ciba-id1-client-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        ciba_mode: "ping",
                        fapi_ciba_profile: "openbanking_brazil",
                      },
                    },
                  },
                ],
              },
            },
            {
              id: "op",
              label: "OP (Authorization Server)",
              next: {
                id: "plan",
                question: "Which certification plan are you creating?",
                choices: [
                  {
                    id: "fapi1_brazil_op",
                    label: "FAPI Security Profile",
                    description: "Server-side FAPI tests for Open Finance Brazil",
                    result: {
                      plan_name: "fapi1-advanced-final-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_auth_request_method: "pushed",
                        fapi_profile: "openbanking_brazil",
                        fapi_response_mode: "plain_response",
                      },
                      also_required: [
                        { id: "dcr_brazil_op", label: "Dynamic Client Registration" },
                      ],
                    },
                  },
                  {
                    id: "dcr_brazil_op",
                    label: "Dynamic Client Registration",
                    description:
                      "DCR tests — also required for Open Finance Brazil OP certification",
                    result: {
                      plan_name: "fapi1-advanced-final-brazil-dcr-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_auth_request_method: "pushed",
                        fapi_response_mode: "plain_response",
                        fapi_profile: "openbanking_brazil",
                      },
                      // Data fix vs the YAML: was `fapi2_brazil_op`, which does
                      // not exist in this step. Symmetric with
                      // fapi1_brazil_op → dcr_brazil_op above.
                      also_required: [{ id: "fapi1_brazil_op", label: "FAPI Security Profile" }],
                    },
                  },
                  {
                    id: "ciba",
                    label: "CIBA",
                    description: "CIBA tests",
                    result: {
                      plan_name: "fapi-ciba-id1-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_ciba_profile: "openbanking_brazil",
                        ciba_mode: "ping",
                        client_registration: "static_client",
                      },
                    },
                  },
                ],
              },
            },
          ],
        },
      ],
    },
    {
      id: "open_insurance_brazil",
      label: "🇧🇷 OpenInsurance Brazil",
      steps: [
        {
          id: "role",
          question: "What is your role?",
          choices: [
            {
              id: "rp",
              label: "RP (Client)",
              result: {
                plan_name: "fapi1-advanced-final-client-test-plan",
                variants: {
                  client_auth_type: "private_key_jwt",
                  fapi_profile: "openinsurance_brazil",
                  fapi_auth_request_method: "pushed",
                  fapi_response_mode: "plain_response",
                  fapi_client_type: "oidc",
                },
              },
            },
            {
              id: "op",
              label: "OP (Authorization Server)",
              result: {
                plan_name: "fapi1-advanced-final-test-plan",
                variants: {
                  client_auth_type: "private_key_jwt",
                  fapi_auth_request_method: "pushed",
                  fapi_profile: "openinsurance_brazil",
                  fapi_response_mode: "plain_response",
                },
              },
            },
          ],
        },
      ],
    },
    {
      id: "open_banking_uk",
      label: "🇬🇧 Openbanking UK",
      steps: [
        {
          id: "role",
          question: "What is your role?",
          choices: [
            {
              id: "rp",
              label: "RP (Client)",
              next: {
                id: "client_auth",
                question: "Client authentication method?",
                choices: [
                  {
                    id: "pkjwt",
                    label: "private_key_jwt",
                    result: {
                      plan_name: "fapi1-advanced-final-client-test-plan",
                      variants: {
                        fapi_profile: "openbanking_uk",
                        fapi_auth_request_method: "by_value",
                        fapi_response_mode: "plain_response",
                        client_auth_type: "private_key_jwt",
                      },
                    },
                  },
                  {
                    id: "mtls",
                    label: "mTLS",
                    result: {
                      plan_name: "fapi1-advanced-final-client-test-plan",
                      variants: {
                        fapi_profile: "openbanking_uk",
                        fapi_auth_request_method: "by_value",
                        fapi_response_mode: "plain_response",
                        client_auth_type: "mtls",
                      },
                    },
                  },
                ],
              },
            },
            {
              id: "op",
              label: "OP (Authorization Server)",
              next: {
                id: "client_auth",
                question: "Client authentication method accepted by your server?",
                choices: [
                  {
                    id: "pkjwt",
                    label: "private_key_jwt",
                    result: {
                      plan_name: "fapi1-advanced-final-test-plan",
                      variants: {
                        fapi_profile: "openbanking_uk",
                        fapi_auth_request_method: "by_value",
                        fapi_response_mode: "plain_response",
                        client_auth_type: "private_key_jwt",
                      },
                    },
                  },
                  {
                    id: "mtls",
                    label: "mTLS",
                    result: {
                      plan_name: "fapi1-advanced-final-test-plan",
                      variants: {
                        fapi_profile: "openbanking_uk",
                        fapi_auth_request_method: "by_value",
                        fapi_response_mode: "plain_response",
                        client_auth_type: "mtls",
                      },
                    },
                  },
                ],
              },
            },
          ],
        },
      ],
    },
    {
      id: "cdr_au",
      label: "🇦🇺 CDR Australia",
      steps: [
        {
          id: "role",
          question: "What is your role?",
          choices: [
            {
              id: "rp",
              label: "RP (Client)",
              result: {
                plan_name: "fapi1-advanced-final-client-test-plan",
                variants: {
                  client_auth_type: "private_key_jwt",
                  fapi_profile: "consumerdataright_au",
                  fapi_auth_request_method: "pushed",
                  fapi_response_mode: "plain_response",
                  fapi_client_type: "oidc",
                },
              },
            },
            {
              id: "op",
              label: "OP (Authorization Server)",
              result: {
                plan_name: "fapi1-advanced-final-test-plan",
                variants: {
                  client_auth_type: "private_key_jwt",
                  fapi_profile: "consumerdataright_au",
                  fapi_auth_request_method: "pushed",
                  fapi_response_mode: "jarm",
                },
              },
            },
          ],
        },
      ],
    },
    {
      id: "connectid_au",
      label: "🇦🇺 ConnectID (Australia)",
      steps: [
        {
          id: "role",
          question: "What is your role?",
          choices: [
            {
              id: "rp",
              label: "RP (Client)",
              next: {
                id: "plan",
                question: "Which certification plan are you creating?",
                choices: [
                  {
                    id: "fapi2_connectid_rp",
                    label: "FAPI2 Message Signing client",
                    description: "Client-side FAPI2 Message Signing tests for ConnectID.",
                    result: {
                      plan_name: "fapi2-message-signing-final-client-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_profile: "connectid_au",
                        fapi_request_method: "signed_non_repudiation",
                        authorization_request_type: "simple",
                        sender_constrain: "mtls",
                        fapi_response_mode: "plain_response",
                        fapi_client_type: "oidc",
                      },
                    },
                  },
                  {
                    id: "ciba",
                    label: "CIBA client",
                    description: "Client-side CIBA tests for ConnectID.",
                    result: {
                      plan_name: "fapi-ciba-id1-client-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        ciba_mode: "poll",
                        fapi_ciba_profile: "connectid_au",
                      },
                    },
                  },
                ],
              },
            },
            {
              id: "op",
              label: "OP (Authorization Server)",
              next: {
                id: "plan",
                question: "Which certification plan are you creating?",
                choices: [
                  {
                    id: "fapi2_connectid_op",
                    label: "FAPI2 Message Signing",
                    description: "Authorization-server FAPI2 Message Signing tests for ConnectID.",
                    result: {
                      plan_name: "fapi2-message-signing-final-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_profile: "connectid_au",
                        fapi_request_method: "signed_non_repudiation",
                        authorization_request_type: "simple",
                        sender_constrain: "mtls",
                        fapi_response_mode: "plain_response",
                        openid: "openid_connect",
                      },
                    },
                  },
                  {
                    id: "ciba",
                    label: "CIBA",
                    description: "CIBA tests for ConnectID.",
                    result: {
                      plan_name: "fapi-ciba-id1-test-plan",
                      variants: {
                        client_auth_type: "private_key_jwt",
                        fapi_ciba_profile: "connectid_au",
                        ciba_mode: "poll",
                        client_registration: "static_client",
                      },
                    },
                  },
                ],
              },
            },
          ],
        },
      ],
    },
    {
      id: "cbuae",
      label: "🇦🇪 CBUAE (UAE)",
      steps: [
        {
          id: "role",
          question: "What is your role?",
          choices: [
            {
              id: "rp",
              label: "RP (Client)",
              result: {
                plan_name: "fapi2-message-signing-id1-client-test-plan",
                variants: {
                  client_auth_type: "private_key_jwt",
                  fapi_profile: "cbuae",
                  fapi_request_method: "signed_non_repudiation",
                  authorization_request_type: "rar",
                  sender_constrain: "mtls",
                  fapi_response_mode: "plain_response",
                  fapi_client_type: "oidc",
                },
              },
            },
            {
              id: "op",
              label: "OP (Authorization Server)",
              result: {
                plan_name: "fapi2-message-signing-id1-test-plan",
                variants: {
                  client_auth_type: "private_key_jwt",
                  fapi_profile: "cbuae",
                  fapi_request_method: "signed_non_repudiation",
                  authorization_request_type: "rar",
                  sender_constrain: "mtls",
                  fapi_response_mode: "plain_response",
                  openid: "openid_connect",
                },
              },
            },
          ],
        },
      ],
    },
    {
      id: "ksa",
      label: "🇸🇦 KSA (Saudi Arabia)",
      steps: [
        {
          id: "role",
          question: "What is your role?",
          choices: [
            {
              id: "rp",
              label: "RP (Client)",
              next: {
                id: "client_auth",
                question: "Client authentication method?",
                choices: [
                  {
                    id: "pkjwt",
                    label: "private_key_jwt",
                    next: {
                      id: "ksa_spec_version",
                      question: "Which version of the specification?",
                      choices: [
                        {
                          id: "ksav1",
                          label: "SAMA version 1",
                          result: {
                            plan_name: "fapi1-advanced-final-client-test-plan",
                            variants: {
                              client_auth_type: "private_key_jwt",
                              fapi_auth_request_method: "pushed",
                              fapi_client_type: "oidc",
                              fapi_profile: "openbanking_ksa",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                        {
                          id: "ksav2",
                          label: "SAMA version 2",
                          result: {
                            plan_name: "fapi2-message-signing-final-client-test-plan",
                            variants: {
                              client_auth_type: "private_key_jwt",
                              sender_constrain: "mtls",
                              authorization_request_type: "simple",
                              fapi_request_method: "signed_non_repudiation",
                              fapi_client_type: "oidc",
                              fapi_profile: "ksa",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                      ],
                    },
                  },
                  {
                    id: "mtls",
                    label: "mTLS",
                    next: {
                      id: "ksa_spec_version",
                      question: "Which version of the specification?",
                      choices: [
                        {
                          id: "ksav1",
                          label: "SAMA version 1",
                          result: {
                            plan_name: "fapi1-advanced-final-client-test-plan",
                            variants: {
                              client_auth_type: "mtls",
                              fapi_auth_request_method: "pushed",
                              fapi_client_type: "oidc",
                              fapi_profile: "openbanking_ksa",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                        {
                          id: "ksav2",
                          label: "SAMA version 2",
                          result: {
                            plan_name: "fapi2-message-signing-final-client-test-plan",
                            variants: {
                              client_auth_type: "mtls",
                              sender_constrain: "mtls",
                              authorization_request_type: "simple",
                              fapi_request_method: "signed_non_repudiation",
                              fapi_client_type: "oidc",
                              fapi_profile: "ksa",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                      ],
                    },
                  },
                ],
              },
            },
            {
              id: "op",
              label: "OP (Authorization Server)",
              next: {
                id: "client_auth",
                question: "Client authentication method accepted by your server?",
                choices: [
                  {
                    id: "pkjwt",
                    label: "private_key_jwt",
                    next: {
                      id: "ksa_spec_version",
                      question: "Which version of the specification?",
                      choices: [
                        {
                          id: "ksav1",
                          label: "SAMA version 1",
                          result: {
                            plan_name: "fapi1-advanced-final-test-plan",
                            variants: {
                              fapi_profile: "openbanking_ksa",
                              client_auth_type: "private_key_jwt",
                              fapi_auth_request_method: "pushed",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                        {
                          id: "ksav2",
                          label: "SAMA version 2",
                          result: {
                            plan_name: "fapi2-message-signing-final-test-plan",
                            variants: {
                              client_auth_type: "private_key_jwt",
                              sender_constrain: "mtls",
                              authorization_request_type: "simple",
                              openid: "openid_connect",
                              fapi_request_method: "signed_non_repudiation",
                              fapi_profile: "ksa",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                      ],
                    },
                  },
                  {
                    id: "mtls",
                    label: "mTLS",
                    next: {
                      id: "ksa_spec_version",
                      question: "Which version?",
                      choices: [
                        {
                          id: "ksav1",
                          label: "SAMA version 1",
                          result: {
                            plan_name: "fapi1-advanced-final-test-plan",
                            variants: {
                              fapi_profile: "openbanking_ksa",
                              client_auth_type: "mtls",
                              fapi_auth_request_method: "pushed",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                        {
                          id: "ksav2",
                          label: "SAMA version 2",
                          result: {
                            plan_name: "fapi2-message-signing-final-test-plan",
                            variants: {
                              client_auth_type: "mtls",
                              sender_constrain: "mtls",
                              authorization_request_type: "simple",
                              openid: "openid_connect",
                              fapi_request_method: "signed_non_repudiation",
                              fapi_profile: "ksa",
                              fapi_response_mode: "plain_response",
                            },
                          },
                        },
                      ],
                    },
                  },
                ],
              },
            },
          ],
        },
      ],
    },
  ],
};
