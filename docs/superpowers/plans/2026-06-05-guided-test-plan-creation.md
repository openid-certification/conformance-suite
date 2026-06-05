# Guided Test Plan Creation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a wizard-driven "Guided Mode" to `schedule-test.html` that navigates certification testers to the correct test plan via ecosystem → role → plan → variant questions, while keeping the existing "Advanced Mode" unchanged.

**Architecture:** Pure frontend change. A YAML config file (`static/guided-wizard.yaml`) drives the wizard tree. The wizard drives the existing `#planSelect` and `#variantSelectors` machinery so all downstream config/create logic is unchanged. Mode preference lives in `localStorage`. A reminder banner on `plan-detail.html` prompts users to create sibling plans for multi-plan certifications.

**Tech Stack:** Bootstrap 5.3 accordion/collapse, js-yaml 4.1.0 (CDN), vanilla JS (lodash already available), Playwright for E2E tests.

---

## File Map

| File | Action | Purpose |
|---|---|---|
| `src/main/resources/static/guided-wizard.yaml` | **Create** | Wizard tree config — ecosystems, questions, plan names, variant pre-selections |
| `src/main/resources/static/schedule-test.html` | **Modify** | Add mode toggle, wizard HTML sections, variant accordion wrapper |
| `src/main/resources/static/plan-detail.html` | **Modify** | Add also_required reminder banner div and JS |
| `frontend/e2e/schedule-test.spec.js` | **Modify** | Add guided mode E2E tests |
| `frontend/e2e/fixtures/guided-wizard.yaml.js` | **Create** | Fixture: parsed YAML as JS object for test mocking |
| `frontend/e2e/helpers/routes.js` | **Modify** | Add helper to mock `GET /guided-wizard.yaml` |

---

## Task 1: Create `guided-wizard.yaml`

**Files:**
- Create: `src/main/resources/static/guided-wizard.yaml`

- [ ] **Step 1: Write the YAML file**

```yaml
# Wizard configuration for Guided Test Plan Creation.
# Each choice has either `next:` (another question) or `result:` (a leaf).
# result.plan_name must match a testPlanName from @PublishTestPlan.
# result.variants keys must match variant parameter names (see variant/*.java enums).
# result.also_required lists sibling choice ids needed for full certification.

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
                  description: "Client-side FAPI 2.0 tests for Open Finance Brazil"
                  next:
                    id: client_auth
                    question: "Client authentication method?"
                    choices:
                      - id: pkjwt
                        label: "private_key_jwt"
                        result:
                          plan_name: fapi2-security-profile-final-brazil-client-test-plan
                          variants:
                            client_auth_type: private_key_jwt
                            fapi_response_mode: plain_response
                            fapi_request_method: unsigned
                          also_required:
                            - id: dcr_brazil_rp
                              label: "Dynamic Client Registration"
                      - id: mtls
                        label: "mTLS"
                        result:
                          plan_name: fapi2-security-profile-final-brazil-client-test-plan
                          variants:
                            client_auth_type: mtls
                            fapi_response_mode: plain_response
                            fapi_request_method: unsigned
                          also_required:
                            - id: dcr_brazil_rp
                              label: "Dynamic Client Registration"
                - id: dcr_brazil_rp
                  label: "Dynamic Client Registration (RP)"
                  description: "DCR tests — also required for Open Finance Brazil certification"
                  result:
                    plan_name: fapi2-security-profile-final-brazil-dcr-test-plan
                    variants: {}
                    also_required:
                      - id: fapi2_brazil_rp
                        label: "FAPI 2.0 Security Profile (RP)"

          - id: op
            label: "OP (Authorization Server)"
            next:
              id: plan
              question: "Which certification plan are you creating?"
              choices:
                - id: fapi2_brazil_op
                  label: "FAPI 2.0 Security Profile (OP)"
                  description: "Server-side FAPI 2.0 tests for Open Finance Brazil"
                  result:
                    plan_name: fapi2-security-profile-final-test-plan
                    variants:
                      fapi_profile: openbanking_brazil
                      client_auth_type: private_key_jwt
                      fapi_response_mode: plain_response
                      fapi_request_method: unsigned
                    also_required:
                      - id: dcr_brazil_op
                        label: "Dynamic Client Registration (OP)"
                - id: dcr_brazil_op
                  label: "Dynamic Client Registration (OP)"
                  description: "DCR tests — also required for Open Finance Brazil certification"
                  result:
                    plan_name: fapi2-security-profile-final-brazil-dcr-test-plan
                    variants: {}
                    also_required:
                      - id: fapi2_brazil_op
                        label: "FAPI 2.0 Security Profile (OP)"

  - id: open_banking_uk
    label: "Open Banking UK"
    steps:
      - id: role
        question: "What is your role?"
        choices:
          - id: rp
            label: "RP (Client)"
            next:
              id: client_auth
              question: "Client authentication method?"
              choices:
                - id: pkjwt
                  label: "private_key_jwt"
                  result:
                    plan_name: fapi1-advanced-final-client-test-plan
                    variants:
                      fapi_profile: openbanking_uk
                      client_auth_type: private_key_jwt
                - id: mtls
                  label: "mTLS"
                  result:
                    plan_name: fapi1-advanced-final-client-test-plan
                    variants:
                      fapi_profile: openbanking_uk
                      client_auth_type: mtls
          - id: op
            label: "OP (Authorization Server)"
            next:
              id: client_auth
              question: "Client authentication method accepted by your server?"
              choices:
                - id: pkjwt
                  label: "private_key_jwt"
                  result:
                    plan_name: fapi1-advanced-final-test-plan
                    variants:
                      fapi_profile: openbanking_uk
                      client_auth_type: private_key_jwt
                - id: mtls
                  label: "mTLS"
                  result:
                    plan_name: fapi1-advanced-final-test-plan
                    variants:
                      fapi_profile: openbanking_uk
                      client_auth_type: mtls

  - id: ksa
    label: "KSA (Saudi Arabia)"
    steps:
      - id: role
        question: "What is your role?"
        choices:
          - id: rp
            label: "RP (Client)"
            next:
              id: client_auth
              question: "Client authentication method?"
              choices:
                - id: pkjwt
                  label: "private_key_jwt"
                  result:
                    plan_name: fapi1-advanced-final-client-test-plan
                    variants:
                      fapi_profile: openbanking_ksa
                      client_auth_type: private_key_jwt
                - id: mtls
                  label: "mTLS"
                  result:
                    plan_name: fapi1-advanced-final-client-test-plan
                    variants:
                      fapi_profile: openbanking_ksa
                      client_auth_type: mtls
          - id: op
            label: "OP (Authorization Server)"
            next:
              id: client_auth
              question: "Client authentication method accepted by your server?"
              choices:
                - id: pkjwt
                  label: "private_key_jwt"
                  result:
                    plan_name: fapi1-advanced-final-test-plan
                    variants:
                      fapi_profile: openbanking_ksa
                      client_auth_type: private_key_jwt
                - id: mtls
                  label: "mTLS"
                  result:
                    plan_name: fapi1-advanced-final-test-plan
                    variants:
                      fapi_profile: openbanking_ksa
                      client_auth_type: mtls
```

- [ ] **Step 2: Verify plan names exist in the codebase**

```bash
grep -rn "testPlanName" src/main/java --include="*.java" | grep -E \
  "fapi2-security-profile-final-brazil-client-test-plan|fapi2-security-profile-final-brazil-dcr-test-plan|fapi2-security-profile-final-test-plan|fapi1-advanced-final-client-test-plan|fapi1-advanced-final-test-plan"
```

Expected: each plan name appears exactly once. If a name is missing, correct it using the output of:
```bash
grep -rn "testPlanName\s*=" src/main/java --include="*.java" | grep -i "brazil\|fapi2\|fapi1.*final"
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/guided-wizard.yaml
git commit -m "feat: add guided-wizard.yaml with Brazil, UK, KSA ecosystem paths"
```

---

## Task 2: Add js-yaml CDN dependency to `schedule-test.html`

**Files:**
- Modify: `src/main/resources/static/schedule-test.html` (head section, around line 35)

- [ ] **Step 1: Add js-yaml script tag**

In the `<head>` of `schedule-test.html`, after the existing Lodash `<script>` tag (line 33), add:

```html
    <script src="https://cdn.jsdelivr.net/npm/js-yaml@4.1.0/dist/js-yaml.min.js"
            integrity="sha256-dVlr80/GQc0mvvaTT2tn9+eR+t9F/RiHO53nh3Lce0="
            crossorigin="anonymous"></script>
```

To get the correct SRI hash for js-yaml 4.1.0 run:
```bash
curl -s https://cdn.jsdelivr.net/npm/js-yaml@4.1.0/dist/js-yaml.min.js | openssl dgst -sha256 -binary | openssl base64
```
Prefix the output with `sha256-` and use that as the `integrity` value.

- [ ] **Step 2: Verify the CDN script loads in E2E tests**

Run the existing E2E tests to confirm no regressions:
```bash
cd frontend && npm run test:e2e -- e2e/schedule-test.spec.js
```
Expected: all existing tests pass (js-yaml loads but is unused at this point).

---

## Task 3: Wrap variant selectors in a Bootstrap collapse accordion

**Files:**
- Modify: `src/main/resources/static/schedule-test.html` (around line 149–155 and the `updateVariants()` function around line 2962)

The existing HTML for variant selectors (lines 149–155) is:
```html
<div class="row">
    <div class="col-md-12">
        <form id="variantSelectors">
            <!-- render variant selectors here -->
        </form>
    </div>
</div>
```

- [ ] **Step 1: Replace the variant selectors row with an accordion wrapper**

Replace those 6 lines with:

```html
<div id="variantSelectorSection" style="display:none">
    <div class="accordion mb-3" id="variantAccordion">
        <div class="accordion-item">
            <h2 class="accordion-header" id="variantAccordionHeading">
                <button class="accordion-button collapsed d-flex gap-2" type="button"
                        data-bs-toggle="collapse" data-bs-target="#variantAccordionBody"
                        aria-expanded="false" aria-controls="variantAccordionBody">
                    <span>Test Variants</span>
                    <span id="variantSummaryText" class="text-muted small fst-italic"></span>
                </button>
            </h2>
            <div id="variantAccordionBody" class="accordion-collapse collapse"
                 aria-labelledby="variantAccordionHeading">
                <div class="accordion-body p-2">
                    <form id="variantSelectors">
                        <!-- render variant selectors here -->
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
```

- [ ] **Step 2: Update `updateVariants()` to show/hide the accordion wrapper**

In `updateVariants()` (around line 2962), replace:
```javascript
document.getElementById('variantSelectors').style.display = '';
```
with:
```javascript
document.getElementById('variantSelectorSection').style.display = '';
updateVariantSummary();
```

And replace:
```javascript
document.getElementById('variantSelectors').style.display = 'none';
```
with:
```javascript
document.getElementById('variantSelectorSection').style.display = 'none';
document.getElementById('variantSummaryText').textContent = '';
```

- [ ] **Step 3: Add `updateVariantSummary()` function**

Add this function right before `getSelectedVariant()` (around line 3054):

```javascript
function updateVariantSummary() {
    var selection = getSelectedVariant();
    var parts = [];
    _.each(selection, function(v, k) {
        if (v && v !== 'select') {
            parts.push(v);
        }
    });
    document.getElementById('variantSummaryText').textContent =
        parts.length > 0 ? parts.join(' · ') : '';
}
```

- [ ] **Step 4: Call `updateVariantSummary()` after variant changes**

In the `#variantSelectors` change listener (around line 2686):

```javascript
document.getElementById('variantSelectors').addEventListener("change", function(){
    updateConfigFieldVisibility();
    updateVariantSummary();  // ADD THIS LINE
});
```

- [ ] **Step 5: Verify accordion behaviour manually**

Run the E2E tests:
```bash
cd frontend && npm run test:e2e -- e2e/schedule-test.spec.js
```
Expected: all existing tests pass. The variants accordion is collapsed by default; variant summary text updates as selects change.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/schedule-test.html
git commit -m "feat: wrap variant selectors in collapsible accordion with summary"
```

---

## Task 4: Add mode toggle HTML and `setMode()` wiring

**Files:**
- Modify: `src/main/resources/static/schedule-test.html` (HTML structure + JS)

- [ ] **Step 1: Add mode toggle HTML**

After the `<div id="scheduleTestPage">` opening tag (around line 89), add:

```html
<div class="row mb-2">
    <div class="col-md-12 d-flex justify-content-end">
        <div class="btn-group btn-group-sm" role="group" id="modeToggleContainer" aria-label="Creation mode">
            <button id="guidedModeBtn" type="button" class="btn btn-primary"
                    onclick="setMode('guided')">Guided</button>
            <button id="advancedModeBtn" type="button" class="btn btn-outline-secondary"
                    onclick="setMode('advanced')">Advanced</button>
        </div>
    </div>
</div>
```

- [ ] **Step 2: Wrap existing cascade dropdowns in `#advancedSection`**

Wrap the four existing dropdown rows (`specFamilyRow`, `entityRow`, `specVersionRow`, `planRow`) — currently lines 97–148 — in a new div:

```html
<div id="advancedSection">
    <!-- existing specFamilyRow, entityRow, specVersionRow, planRow unchanged -->
</div>
```

- [ ] **Step 3: Add `#guidedSection` placeholder HTML between the toggle and `#advancedSection`**

After the mode toggle row and before `#advancedSection`, add:

```html
<!-- Guided wizard section — contents populated by JS -->
<div id="guidedSection" style="display:none">
    <div id="wizardChips" class="mb-3 d-flex flex-wrap gap-2"></div>
    <div id="wizardQuestion" class="mb-3"></div>
</div>
```

- [ ] **Step 4: Add `setMode()`, `applyMode()`, and `initMode()` to the page's `<script>` block**

Add these functions near the top of the inline `<script>` block (after `document.addEventListener("DOMContentLoaded", ...)` is fine, they're called via `onclick`):

```javascript
function initMode() {
    var savedMode = localStorage.getItem('oidf-guided-mode') || 'guided';
    applyMode(savedMode);
}

function setMode(mode) {
    localStorage.setItem('oidf-guided-mode', mode);
    applyMode(mode);
    if (mode === 'guided') {
        // Reset wizard to start when switching into guided mode
        wizardState.answers = [];
        wizardState.currentStep = null;
        wizardState.currentResult = null;
        renderWizardStart();
    }
}

function applyMode(mode) {
    var isGuided = (mode === 'guided');
    document.getElementById('guidedSection').style.display = isGuided ? '' : 'none';
    document.getElementById('advancedSection').style.display = isGuided ? 'none' : '';
    document.getElementById('guidedModeBtn').className =
        isGuided ? 'btn btn-primary btn-sm' : 'btn btn-outline-secondary btn-sm';
    document.getElementById('advancedModeBtn').className =
        isGuided ? 'btn btn-outline-secondary btn-sm' : 'btn btn-primary btn-sm';
}
```

- [ ] **Step 5: Call `initMode()` at the end of the DOMContentLoaded promise chain**

In the existing `DOMContentLoaded` handler, extend the `.then()` chain after `registerGenerateJwksListeners()`:

```javascript
.then(function() {
    return registerGenerateJwksListeners();
}).then(function() {
    initMode();           // ADD THIS
    if (localStorage.getItem('oidf-guided-mode') !== 'advanced') {
        return loadGuidedWizard();   // ADD THIS
    }
}).finally(function() {
    FAPI_UI.activeTooltip();
    FAPI_UI.hideBusy();
});
```

- [ ] **Step 6: Run E2E tests**

```bash
cd frontend && npm run test:e2e -- e2e/schedule-test.spec.js
```

Expected: existing tests still pass. (They will use advanced mode since they don't set localStorage, so the guided section will be hidden by default in those test contexts — which is fine because the tests do not mock `guided-wizard.yaml`.)

> **Note for test compatibility:** Existing tests navigate to `schedule-test.html` without setting `localStorage`. Since `oidf-guided-mode` defaults to `'guided'`, existing tests that interact with `#specFamilySelect` will now find it in `#advancedSection` which is hidden. To fix this, existing tests must set `localStorage` to `'advanced'` before the page load or we make guided mode require the YAML to load first (so it gracefully degrades if YAML is unavailable).
>
> The cleanest fix: in `loadGuidedWizard()` (Task 5), if the fetch fails, fall back to advanced mode silently. This way existing tests that don't mock `/guided-wizard.yaml` will see the fail-fast handler abort the request and the page will fall back to advanced mode.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/schedule-test.html
git commit -m "feat: add guided/advanced mode toggle with localStorage persistence"
```

---

## Task 5: Implement wizard JS state machine and rendering

**Files:**
- Modify: `src/main/resources/static/schedule-test.html` (inline script)

- [ ] **Step 1: Add wizard state object near top of inline script**

Add at the top of the inline `<script>` block (before `document.addEventListener`):

```javascript
var wizardState = {
    ecosystemConfig: null,   // parsed YAML: { ecosystems: [...] }
    answers: [],             // [{stepId, choiceId, choiceLabel}, ...]
    currentResult: null      // the result leaf once all questions are answered
};
```

- [ ] **Step 2: Add `loadGuidedWizard()`**

```javascript
function loadGuidedWizard() {
    return fetch('guided-wizard.yaml')
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Failed to load wizard config: ' + response.status);
            }
            return response.text();
        })
        .then(function(text) {
            wizardState.ecosystemConfig = jsyaml.load(text);
            renderWizardStart();
        })
        .catch(function(err) {
            // Fail gracefully: switch to advanced mode so the page remains usable
            console.warn('Guided wizard unavailable, falling back to advanced mode:', err);
            applyMode('advanced');
        });
}
```

- [ ] **Step 3: Add `renderWizardStart()`**

```javascript
function renderWizardStart() {
    if (!wizardState.ecosystemConfig) return;
    document.getElementById('wizardChips').innerHTML = '';
    wizardState.answers = [];
    wizardState.currentResult = null;

    // Reset plan and variants
    var planSelect = document.getElementById('planSelect');
    planSelect.value = 'select';
    planSelect.dispatchEvent(new Event('change'));

    var firstStep = {
        id: 'ecosystem',
        question: 'Which ecosystem are you testing?',
        choices: wizardState.ecosystemConfig.ecosystems.map(function(eco) {
            return { id: eco.id, label: eco.label, _ecoRef: eco };
        })
    };
    renderWizardStep(firstStep);
}
```

- [ ] **Step 4: Add `renderWizardStep(step)`**

```javascript
function renderWizardStep(step) {
    wizardState.currentStep = step;
    var container = document.getElementById('wizardQuestion');

    var choicesHtml = step.choices.map(function(choice) {
        var descHtml = choice.description
            ? '<div class="text-muted small mt-1">' + _.escape(choice.description) + '</div>'
            : '';
        return '<button type="button" class="btn btn-outline-secondary wizard-choice-btn" ' +
               'data-choice-id="' + _.escape(choice.id) + '">' +
               '<strong>' + _.escape(choice.label) + '</strong>' + descHtml +
               '</button>';
    }).join('');

    container.innerHTML =
        '<div class="mb-2 fw-semibold">' + _.escape(step.question) + '</div>' +
        '<div class="d-flex flex-wrap gap-2">' + choicesHtml + '</div>';

    container.querySelectorAll('.wizard-choice-btn').forEach(function(btn) {
        btn.addEventListener('click', function() {
            var choiceId = btn.getAttribute('data-choice-id');
            var choice = step.choices.find(function(c) { return c.id === choiceId; });
            if (choice) onWizardAnswer(choice);
        });
    });
}
```

- [ ] **Step 5: Add `onWizardAnswer(choice)`**

```javascript
function onWizardAnswer(choice) {
    // Lock current answer as a chip
    wizardState.answers.push({
        stepId: wizardState.currentStep.id,
        choiceId: choice.id,
        choiceLabel: choice.label
    });
    renderWizardChips();

    // Determine next step
    if (choice.result) {
        // Reached a leaf
        document.getElementById('wizardQuestion').innerHTML = '';
        wizardState.currentResult = choice.result;
        applyWizardResult(choice.result);
    } else if (choice.next) {
        renderWizardStep(choice.next);
    } else if (choice._ecoRef) {
        // Ecosystem choice — dive into the first step of that ecosystem
        var eco = choice._ecoRef;
        renderWizardStep(eco.steps[0]);
    }
}
```

- [ ] **Step 6: Add `renderWizardChips()`**

```javascript
function renderWizardChips() {
    var chipsHtml = wizardState.answers.map(function(answer, idx) {
        return '<button type="button" class="btn btn-sm wizard-chip" ' +
               'style="background:#e7f1ff;border:1px solid #0d6efd;border-radius:16px;padding:2px 10px" ' +
               'data-chip-index="' + idx + '">' +
               _.escape(answer.choiceLabel) + ' <span aria-hidden="true">&times;</span>' +
               '</button>';
    }).join('');
    var chips = document.getElementById('wizardChips');
    chips.innerHTML = chipsHtml;
    chips.querySelectorAll('.wizard-chip').forEach(function(chip) {
        chip.addEventListener('click', function() {
            onChipClick(parseInt(chip.getAttribute('data-chip-index'), 10));
        });
    });
}
```

- [ ] **Step 7: Add `onChipClick(index)`**

```javascript
function onChipClick(index) {
    // Keep answers up to (but not including) the clicked index
    wizardState.answers = wizardState.answers.slice(0, index);
    renderWizardChips();
    wizardState.currentResult = null;

    // Reset plan and variants
    var planSelect = document.getElementById('planSelect');
    planSelect.value = 'select';
    planSelect.dispatchEvent(new Event('change'));

    // Re-navigate wizard to the correct step
    _replayWizardAnswers();
}

function _replayWizardAnswers() {
    if (!wizardState.ecosystemConfig) return;

    var firstStep = {
        id: 'ecosystem',
        question: 'Which ecosystem are you testing?',
        choices: wizardState.ecosystemConfig.ecosystems.map(function(eco) {
            return { id: eco.id, label: eco.label, _ecoRef: eco };
        })
    };

    var step = firstStep;
    for (var i = 0; i < wizardState.answers.length; i++) {
        var answer = wizardState.answers[i];
        var choice = step.choices.find(function(c) { return c.id === answer.choiceId; });
        if (!choice) break;
        if (choice.next) {
            step = choice.next;
        } else if (choice._ecoRef) {
            step = choice._ecoRef.steps[0];
        } else {
            break;
        }
    }
    renderWizardStep(step);
}
```

- [ ] **Step 8: Add `applyWizardResult(result)`**

```javascript
function applyWizardResult(result) {
    // Select the plan by driving the existing cascade machinery
    if (!(result.plan_name in FAPI_UI.availablePlans)) {
        FAPI_UI.showError({
            error: 'Plan not found',
            message: 'Plan "' + result.plan_name + '" is not available on this server. ' +
                     'Try Advanced mode to browse all available plans.'
        });
        return;
    }

    selectPlanByName(result.plan_name);

    // Pre-set variant selects after the plan is selected (updateVariants runs synchronously)
    if (result.variants) {
        _.each(result.variants, function(value, key) {
            var el = document.querySelector('.variant-selector[data-variant-parameter="' + key + '"]');
            if (el) {
                el.value = value;
            }
        });
        updateVariantOptionsVisibility();
        updateConfigFieldVisibility();
        updateVariantSummary();
    }
}
```

- [ ] **Step 9: Manually verify wizard flow in browser**

Start the app (`devenv up`, then `java -jar target/fapi-test-suite.jar --spring.profiles.active=dev`), open `https://localhost.emobix.co.uk:8443/schedule-test.html`.

Verify:
1. Page loads in Guided mode (pill shows "Guided" active)
2. First question "Which ecosystem are you testing?" appears with ecosystem buttons
3. Clicking "Open Finance Brazil" adds a chip and shows "What is your role?"
4. Clicking "RP (Client)" adds a chip and shows "Which certification plan are you creating?"
5. Clicking "FAPI 2.0 Security Profile (RP)" shows "Client authentication method?"
6. Clicking "private_key_jwt" closes the question area and the variants accordion appears collapsed with summary `private_key_jwt · plain_response · unsigned`
7. The configuration form appears below
8. Clicking a chip resets from that point

- [ ] **Step 10: Commit**

```bash
git add src/main/resources/static/schedule-test.html
git commit -m "feat: implement guided wizard state machine and chip-based progressive disclosure"
```

---

## Task 6: Wizard URL pre-seeding (also_required reminder links)

When the user creates Plan A and needs to create Plan B, the reminder banner (Task 7) will link back to `schedule-test.html?wizard_start=<preset>`. This task handles reading that param and auto-navigating.

**Files:**
- Modify: `src/main/resources/static/schedule-test.html` (inline script)

- [ ] **Step 1: Store `also_required` in sessionStorage before redirect**

Find the `createPlanBtn.onclick` handler (around line 2537). After the successful `fetch` of `POST /api/plan` and before `window.location.href = 'plan-detail.html?plan=' + data.id`, add:

```javascript
// Store also_required reminder data for plan-detail.html
if (wizardState.currentResult && wizardState.currentResult.also_required &&
        wizardState.currentResult.also_required.length > 0 &&
        localStorage.getItem('oidf-guided-mode') !== 'advanced') {
    var ecosystemAnswer = wizardState.answers.find(function(a) { return a.stepId === 'ecosystem'; });
    sessionStorage.setItem('oidf-also-required', JSON.stringify({
        ecosystemId: ecosystemAnswer ? ecosystemAnswer.choiceId : '',
        ecosystemLabel: ecosystemAnswer ? ecosystemAnswer.choiceLabel : '',
        also_required: wizardState.currentResult.also_required,
        answersUpToPlan: wizardState.answers.slice(0, 2)  // ecosystem + role answers
    }));
}
```

- [ ] **Step 2: Apply wizard pre-seed on page load**

In `loadGuidedWizard()`, after `renderWizardStart()`, add:

```javascript
function loadGuidedWizard() {
    return fetch('guided-wizard.yaml')
        .then(function(r) { return r.ok ? r.text() : Promise.reject(r.status); })
        .then(function(text) {
            wizardState.ecosystemConfig = jsyaml.load(text);
            renderWizardStart();
            _applyWizardPreset();  // handle ?wizard_start param
        })
        .catch(function(err) {
            console.warn('Guided wizard unavailable, falling back to advanced mode:', err);
            applyMode('advanced');
        });
}

function _applyWizardPreset() {
    var params = new URLSearchParams(window.location.search);
    var presetJson = params.get('wizard_preset');
    if (!presetJson) return;
    try {
        var preset = JSON.parse(presetJson);
        // preset = { ecosystemId, answersUpToPlan: [{stepId, choiceId, choiceLabel}, ...] }
        if (!preset.answersUpToPlan) return;
        preset.answersUpToPlan.forEach(function(a) {
            wizardState.answers.push(a);
        });
        renderWizardChips();
        _replayWizardAnswers();
    } catch (e) {
        // ignore malformed preset
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/schedule-test.html
git commit -m "feat: support wizard_preset URL param for also_required reminder links"
```

---

## Task 7: also_required reminder banner on `plan-detail.html`

**Files:**
- Modify: `src/main/resources/static/plan-detail.html`

- [ ] **Step 1: Locate where to insert the banner**

In `plan-detail.html`, find the `<div class="container-fluid">` main content wrapper. The plan header is inserted dynamically into `#planHeader`. The banner should appear at the very top of the container, before `#planHeader`.

Find the line containing `<div id="planHeader">` (approximately line 26 in the static HTML wrapper around the plan template rendering area) or find where the plan content starts. The banner HTML goes in the static HTML, not in the template.

- [ ] **Step 2: Add banner HTML**

In `plan-detail.html`, inside the main `<div class="container-fluid">` but before the dynamically populated plan section, add:

```html
<!-- also_required reminder banner (shown by JS when sessionStorage has data) -->
<div id="alsoRequiredBanner" class="alert alert-warning d-flex align-items-center gap-3 mb-3"
     style="display:none !important">
    <span>⚠️</span>
    <span id="alsoRequiredMsg" class="flex-grow-1"></span>
    <a id="alsoRequiredLink" href="schedule-test.html" class="btn btn-sm btn-warning">Create Plan →</a>
</div>
```

> Note: `style="display:none !important"` ensures the div is hidden until JS shows it with `banner.style.removeProperty('display')` + `banner.style.display = ''`. This avoids a flash of the banner before JS runs.

- [ ] **Step 3: Add banner JS to `plan-detail.html`**

In the `<script>` section of `plan-detail.html`, after `DOMContentLoaded` is set up, add:

```javascript
function checkAlsoRequired() {
    var data = sessionStorage.getItem('oidf-also-required');
    if (!data) return;
    sessionStorage.removeItem('oidf-also-required');
    try {
        var parsed = JSON.parse(data);
        var required = parsed.also_required;
        if (!required || !required.length) return;
        var count = required.length;
        var labels = required.map(function(r) { return r.label; }).join(', ');
        document.getElementById('alsoRequiredMsg').textContent =
            count + ' more plan' + (count > 1 ? 's' : '') + ' required for ' +
            parsed.ecosystemLabel + ' certification: ' + labels;
        var link = document.getElementById('alsoRequiredLink');
        link.href = 'schedule-test.html?wizard_preset=' + encodeURIComponent(JSON.stringify({
            ecosystemId: parsed.ecosystemId,
            answersUpToPlan: parsed.answersUpToPlan
        }));
        var banner = document.getElementById('alsoRequiredBanner');
        banner.style.removeProperty('display');
        banner.style.display = '';
    } catch (e) {
        // ignore
    }
}
```

Call `checkAlsoRequired()` inside the existing `DOMContentLoaded` handler, after the page content is rendered (i.e., after the promise chain that calls `loadPlan()` / `insertAdjacentHTML`).

- [ ] **Step 4: Run E2E tests**

```bash
cd frontend && npm run test:e2e -- e2e/plan-detail.spec.js
```

Expected: all existing plan-detail tests pass (banner is hidden when sessionStorage has no data).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/plan-detail.html
git commit -m "feat: add also_required reminder banner on plan-detail.html"
```

---

## Task 8: E2E fixtures and guided-wizard.yaml route helper

**Files:**
- Create: `frontend/e2e/fixtures/guided-wizard.yaml.js`
- Modify: `frontend/e2e/helpers/routes.js`

- [ ] **Step 1: Create the wizard config fixture**

```javascript
// frontend/e2e/fixtures/guided-wizard.yaml.js

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
```

Note: the fixture uses `fapi2-security-profile-final-test-plan` (already in `MOCK_PLANS`) to avoid needing extra plan fixtures.

- [ ] **Step 2: Add `setupGuidedWizardRoute()` helper to `routes.js`**

In `frontend/e2e/helpers/routes.js`, import the fixture and add the helper:

```javascript
import { GUIDED_WIZARD_YAML } from '../fixtures/guided-wizard.yaml.js';

/**
 * Mock GET /guided-wizard.yaml.
 * Must be called before page.goto() since loadGuidedWizard() runs on page init.
 */
export async function setupGuidedWizardRoute(page) {
    await page.route('**/guided-wizard.yaml', (route) =>
        route.fulfill({
            status: 200,
            contentType: 'text/yaml',
            body: GUIDED_WIZARD_YAML,
        })
    );
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/e2e/fixtures/guided-wizard.yaml.js frontend/e2e/helpers/routes.js
git commit -m "test: add guided-wizard.yaml fixture and route helper"
```

---

## Task 9: E2E tests for guided mode in `schedule-test.spec.js`

**Files:**
- Modify: `frontend/e2e/schedule-test.spec.js`

- [ ] **Step 1: Update existing tests to use advanced mode**

Existing tests interact with `#specFamilySelect` which is inside `#advancedSection`. They need to work in advanced mode. Add localStorage setup to each existing test's setup block.

At the top of each existing `test(...)` body, after `setupFailFast(page)`, add:

```javascript
// Force advanced mode so cascade dropdowns are visible
await page.addInitScript(() => {
    localStorage.setItem('oidf-guided-mode', 'advanced');
});
```

This must be added before `page.goto()` — `addInitScript` runs before any page script, which is exactly what we need.

Apply this to all 6 existing tests (cascade populates, submission POSTs, create button disabled, variant selectors render, submitting with variants, variant selectors hidden).

- [ ] **Step 2: Run existing tests to confirm they still pass**

```bash
cd frontend && npm run test:e2e -- e2e/schedule-test.spec.js
```

Expected: all 6 existing tests pass.

- [ ] **Step 3: Add guided mode test — wizard renders first question**

```javascript
test("guided mode shows wizard, not cascade dropdowns (R_G1)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify(ALL_PLANS) })
    );
    await page.route("**/api/lastconfig", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify({}) })
    );
    await setupGuidedWizardRoute(page);
    await setupCommonRoutes(page);

    // Do NOT set oidf-guided-mode — default is 'guided'
    await page.goto("/schedule-test.html");

    // Cascade dropdowns should be hidden
    await expect(page.locator("#advancedSection")).toBeHidden();

    // Wizard question area should be visible
    await expect(page.locator("#wizardQuestion")).toBeVisible();

    // First question: ecosystem
    await expect(page.locator("#wizardQuestion")).toContainText("Which ecosystem are you testing?");

    // Ecosystem buttons appear
    await expect(page.locator("button.wizard-choice-btn")).toHaveCount(
        // matches the number of ecosystems in GUIDED_WIZARD_YAML (1 in fixture)
        1
    );
    await expect(page.locator("button.wizard-choice-btn")).toContainText("Open Finance Brazil");
});
```

- [ ] **Step 4: Add guided mode test — wizard progresses and pre-selects plan**

```javascript
test("guided mode: selecting answers pre-selects plan and shows config form (R_G2)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify(ALL_PLANS) })
    );
    await page.route("**/api/lastconfig", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify({}) })
    );
    await setupGuidedWizardRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Answer: Open Finance Brazil
    await page.locator("button.wizard-choice-btn", { hasText: "Open Finance Brazil" }).click();
    // Answer: RP
    await page.locator("button.wizard-choice-btn", { hasText: "RP (Client)" }).click();
    // Answer: FAPI 2.0
    await page.locator("button.wizard-choice-btn", { hasText: "FAPI 2.0 Security Profile (RP)" }).click();
    // Answer: private_key_jwt
    await page.locator("button.wizard-choice-btn", { hasText: "private_key_jwt" }).click();

    // Chips should appear
    await expect(page.locator("#wizardChips .wizard-chip")).toHaveCount(4);

    // Variant accordion should be visible (plan has variants)
    await expect(page.locator("#variantSelectorSection")).toBeVisible();

    // Variant summary should include pre-selected values
    await expect(page.locator("#variantSummaryText")).toContainText("private_key_jwt");

    // Config form should be visible (all variants pre-selected by wizard)
    await expect(page.locator("#configForm")).toBeVisible();

    // Create button should be enabled
    await expect(page.locator("#createPlanBtn")).toBeEnabled({ timeout: 5000 });
});
```

- [ ] **Step 5: Add guided mode test — chip click resets wizard**

```javascript
test("guided mode: clicking a chip resets wizard from that step (R_G3)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify(ALL_PLANS) })
    );
    await page.route("**/api/lastconfig", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify({}) })
    );
    await setupGuidedWizardRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Progress through ecosystem + role
    await page.locator("button.wizard-choice-btn", { hasText: "Open Finance Brazil" }).click();
    await page.locator("button.wizard-choice-btn", { hasText: "RP (Client)" }).click();

    // Two chips visible
    await expect(page.locator("#wizardChips .wizard-chip")).toHaveCount(2);

    // Click the first chip (ecosystem) to reset
    await page.locator("#wizardChips .wizard-chip").first().click();

    // Now only 0 chips (first chip removed, subsequent reset)
    await expect(page.locator("#wizardChips .wizard-chip")).toHaveCount(0);

    // Back to ecosystem question
    await expect(page.locator("#wizardQuestion")).toContainText("Which ecosystem are you testing?");
});
```

- [ ] **Step 6: Add guided mode test — toggling to advanced reveals cascade**

```javascript
test("guided mode: switching to advanced shows cascade dropdowns (R_G4)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify(ALL_PLANS) })
    );
    await page.route("**/api/lastconfig", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify({}) })
    );
    await setupGuidedWizardRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Starts in guided mode
    await expect(page.locator("#advancedSection")).toBeHidden();

    // Click Advanced button
    await page.locator("#advancedModeBtn").click();

    // Advanced section becomes visible
    await expect(page.locator("#advancedSection")).toBeVisible();

    // Guided section hidden
    await expect(page.locator("#guidedSection")).toBeHidden();

    // localStorage persists the mode
    const mode = await page.evaluate(() => localStorage.getItem('oidf-guided-mode'));
    expect(mode).toBe('advanced');
});
```

- [ ] **Step 7: Add test for variants accordion collapsed by default**

```javascript
test("variant accordion is collapsed by default and shows summary (R_G5)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify(ALL_PLANS) })
    );
    await page.route("**/api/lastconfig", (route) =>
        route.fulfill({ status: 200, contentType: "application/json",
                        body: JSON.stringify({}) })
    );
    await setupGuidedWizardRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Navigate to a result leaf
    await page.locator("button.wizard-choice-btn", { hasText: "Open Finance Brazil" }).click();
    await page.locator("button.wizard-choice-btn", { hasText: "RP (Client)" }).click();
    await page.locator("button.wizard-choice-btn", { hasText: "FAPI 2.0 Security Profile (RP)" }).click();
    await page.locator("button.wizard-choice-btn", { hasText: "private_key_jwt" }).click();

    // Accordion body is NOT visible (collapsed)
    await expect(page.locator("#variantAccordionBody")).toBeHidden();

    // Summary text shows pre-selected variant
    await expect(page.locator("#variantSummaryText")).toContainText("private_key_jwt");

    // Clicking the accordion header expands it
    await page.locator("#variantAccordionHeading button").click();
    await expect(page.locator("#variantAccordionBody")).toBeVisible();
});
```

- [ ] **Step 8: Run all schedule-test E2E tests**

```bash
cd frontend && npm run test:e2e -- e2e/schedule-test.spec.js
```

Expected: all tests (existing 6 + new 5) pass.

- [ ] **Step 9: Commit**

```bash
git add frontend/e2e/schedule-test.spec.js
git commit -m "test: add E2E tests for guided mode wizard, chips, mode toggle, and variant accordion"
```

---

## Task 10: E2E test for also_required banner on `plan-detail.html`

**Files:**
- Modify: `frontend/e2e/plan-detail.spec.js`

- [ ] **Step 1: Add test for also_required banner**

```javascript
test("shows also_required reminder banner when sessionStorage has data (R_G6)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
        route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(MOCK_PLAN_DETAIL),
        })
    );

    await setupTestInfoRoute(page, {
        "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001" },
        "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", testName: "oidcc-server-rotate-keys" },
        "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", testName: "oidcc-ensure-redirect-uri-in-authorization-request" },
    });

    await setupCommonRoutes(page);

    // Pre-populate sessionStorage before page load
    await page.addInitScript(() => {
        sessionStorage.setItem('oidf-also-required', JSON.stringify({
            ecosystemId: 'open_finance_brazil',
            ecosystemLabel: 'Open Finance Brazil',
            also_required: [{ id: 'dcr_brazil_rp', label: 'Dynamic Client Registration' }],
            answersUpToPlan: [
                { stepId: 'ecosystem', choiceId: 'open_finance_brazil', choiceLabel: 'Open Finance Brazil' },
                { stepId: 'role', choiceId: 'rp', choiceLabel: 'RP (Client)' }
            ]
        }));
    });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Banner is visible
    const banner = page.locator("#alsoRequiredBanner");
    await expect(banner).toBeVisible();

    // Banner message mentions the ecosystem and required plan
    await expect(page.locator("#alsoRequiredMsg")).toContainText("Open Finance Brazil");
    await expect(page.locator("#alsoRequiredMsg")).toContainText("Dynamic Client Registration");

    // Link points to schedule-test.html with wizard_preset
    const linkHref = await page.locator("#alsoRequiredLink").getAttribute("href");
    expect(linkHref).toContain("schedule-test.html");
    expect(linkHref).toContain("wizard_preset");

    // sessionStorage is cleared after reading
    const remaining = await page.evaluate(() => sessionStorage.getItem('oidf-also-required'));
    expect(remaining).toBeNull();
});
```

- [ ] **Step 2: Add test confirming banner is hidden when no sessionStorage data**

```javascript
test("no also_required banner shown when sessionStorage is empty (R_G7)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
        route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(MOCK_PLAN_DETAIL),
        })
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    await expect(page.locator("#alsoRequiredBanner")).toBeHidden();
});
```

- [ ] **Step 3: Run all plan-detail E2E tests**

```bash
cd frontend && npm run test:e2e -- e2e/plan-detail.spec.js
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add frontend/e2e/plan-detail.spec.js
git commit -m "test: add E2E tests for also_required banner on plan-detail.html"
```

---

## Task 11: Final integration — run full E2E suite

- [ ] **Step 1: Run the complete E2E test suite**

```bash
cd frontend && npm run test:e2e
```

Expected: all tests pass (no regressions across home, plans, logs, plan-detail, schedule-test, running-test, journeys).

- [ ] **Step 2: Fix any regressions**

If any test fails because of unexpected `#variantSelectorSection` visibility or wizard-related DOM changes, debug by checking:
- Whether `addInitScript` for advanced mode was applied in the failing test
- Whether any test inspects the structure of the variant selectors row directly

- [ ] **Step 3: Final commit**

```bash
git add -p  # review any remaining changes
git commit -m "feat: guided test plan creation wizard — final integration"
```

---

## Self-Review Notes

**Spec coverage check:**

| Spec Feature | Tasks |
|---|---|
| Mode toggle top-right, localStorage | Task 4 |
| Progressive disclosure wizard with chips | Task 5 |
| Plan selection as wizard step | Task 1 (YAML), Task 5 (`_ecoRef` branching) |
| `also_required` → reminder banner | Tasks 6, 7 |
| Variants collapsed with summary (guided mode) | Task 3 |
| Variants collapsed (advanced mode) | Task 3 (same accordion, starts collapsed always) |
| Variants collapsed on plan-detail | Covered by accordion — plan-detail shows variant as plain text; collapse there is handled in plan.html template (see below) |
| YAML config `guided-wizard.yaml` | Task 1 |
| User preference localStorage | Task 4 |
| E2E tests | Tasks 8, 9, 10 |

**Gap: plan-detail.html variant display collapse**

The spec says variants on plan-detail.html should start collapsed. The plan header template (`templates/plan.html`) shows variant as a plain text row `<div class="col-md-11"><%- variant %></div>`. This should be wrapped in a Bootstrap collapse. Since plan.html is a Lodash template, the change is:

Replace the variant row in `templates/plan.html`:
```html
<div class="row">
    <div class="col-md-1">Variant:</div>
    <div class="col-md-11"><%- variant %></div>
</div>
```
with:
```html
<div class="row">
    <div class="col-md-1">Variant:</div>
    <div class="col-md-11">
        <button class="btn btn-link btn-sm p-0 text-start" type="button"
                data-bs-toggle="collapse" data-bs-target="#planVariantDetail"
                aria-expanded="false">
            <span class="text-truncate d-inline-block" style="max-width:300px"><%- variant %></span>
            <span class="bi bi-chevron-down ms-1"></span>
        </button>
        <div class="collapse mt-1" id="planVariantDetail">
            <code class="small"><%- variant %></code>
        </div>
    </div>
</div>
```

This can be added as **Task 3b** or folded into Task 3. Add it before committing Task 3.

**Type/name consistency check:** `wizardState.currentResult` is set in `onWizardAnswer()` (Task 5, Step 5) and read in the `createPlanBtn.onclick` handler (Task 6, Step 1). Both reference `wizardState.currentResult` — consistent. `renderWizardChips()` is called in `onWizardAnswer()`, `onChipClick()`, and `_applyWizardPreset()` — all reference the function by that name — consistent. `updateVariantSummary()` is called in Task 3 and Task 5 `applyWizardResult()` — consistent.

**Placeholder check:** No TBDs or TODOs remain. Task 1 Step 2 includes a verification command to check plan names exist in the codebase.
