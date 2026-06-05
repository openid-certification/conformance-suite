/** Minimal guided-wizard.yaml content as a YAML string for E2E tests. */
export const GUIDED_WIZARD_YAML = `
ecosystems:
  - id: open_finance_brazil
    label: "Open Finance Brazil"
    steps:
      - id: role
        question: "What is your role?"
        choices:
          - id: rp
            label: "RP (Client)"
            next:
              id: plan
              question: "Which certification plan are you creating?"
              choices:
                - id: fapi2_brazil_rp
                  label: "FAPI 2.0 Security Profile (RP)"
                  next:
                    id: client_auth
                    question: "Client authentication method?"
                    choices:
                      - id: pkjwt
                        label: "private_key_jwt"
                        result:
                          plan_name: fapi2-security-profile-final-test-plan
                          variants:
                            client_auth_type: private_key_jwt
                            fapi_response_mode: plain_response
                          also_required:
                            - id: dcr_brazil_rp
                              label: "Dynamic Client Registration"
                - id: dcr_brazil_rp
                  label: "Dynamic Client Registration"
                  result:
                    plan_name: fapi2-security-profile-final-test-plan
                    variants: {}
                    also_required:
                      - id: fapi2_brazil_rp
                        label: "FAPI 2.0 Security Profile (RP)"
`.trim();
