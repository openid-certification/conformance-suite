/**
 * Render the FINAL_ERROR alert into the cts-log-detail-header error
 * slot. Extracted from log-detail.js so Storybook can demonstrate the
 * populated slot using the same code that runs in production — keeps
 * the story honest as the rendering evolves.
 *
 * The /api/runner payload's `error` object uses the legacy field
 * shape consumed by `templates/finalError.html`:
 * `{ error, error_class, stacktrace[], cause_stacktrace[] }`. This
 * function reads it byte-for-byte so v2 paints the same data legacy
 * did. Stacktrace + cause_stacktrace reveal together when the toggle
 * is clicked (mirrors the legacy behavior at log-detail.html where
 * `#stacktrace` and `#causeStacktrace` toggle as a pair).
 *
 * @param {Element|null} slot - The `[data-slot="error"]` element
 *   inside the header (i.e. `#runningTestError`). No-op when the
 *   element is missing.
 * @param {string|object|null|undefined} error - Either a plain string
 *   message or the legacy error object. A nullish value clears the
 *   slot without rendering anything.
 */
export function renderErrorIntoSlot(slot, error) {
  if (!slot) return;
  while (slot.firstChild) slot.removeChild(slot.firstChild);
  if (!error) return;

  const alert = document.createElement("cts-alert");
  alert.setAttribute("variant", "danger");

  if (typeof error === "string") {
    const message = document.createElement("div");
    message.textContent = error;
    alert.appendChild(message);
    slot.appendChild(alert);
    return;
  }

  const heading = document.createElement("strong");
  heading.textContent = "There was an error while running the test:";
  alert.appendChild(heading);

  const summary = document.createElement("em");
  summary.style.marginLeft = "var(--space-2)";
  const errorText = error.error || error.message || "Unknown error";
  summary.textContent = error.error_class ? `${errorText} (${error.error_class})` : `${errorText}`;
  alert.appendChild(summary);
  alert.appendChild(document.createTextNode("."));

  const hasStack = Array.isArray(error.stacktrace) && error.stacktrace.length > 0;
  const hasCause = Array.isArray(error.cause_stacktrace) && error.cause_stacktrace.length > 0;

  if (hasStack || hasCause) {
    // Stacktrace reveal is progressive disclosure of debug detail —
    // it should sit quietly inside the danger alert, not compete with
    // it as a sibling action. ghost + xs gives a low-emphasis text
    // affordance with a chevron, matching the "expand for details"
    // pattern used elsewhere in the suite.
    const toggleBtn = document.createElement("cts-button");
    toggleBtn.id = "stacktraceBtn";
    toggleBtn.setAttribute("variant", "ghost");
    toggleBtn.setAttribute("size", "xs");
    toggleBtn.setAttribute("icon", "chevron-down");
    toggleBtn.setAttribute("label", "Show stacktrace");

    const buildList = (id, items) => {
      const ul = document.createElement("ul");
      ul.id = id;
      ul.style.display = "none";
      ul.style.fontFamily = "var(--font-mono)";
      ul.style.fontSize = "var(--fs-12)";
      ul.style.marginTop = "var(--space-2)";
      for (const line of items) {
        const li = document.createElement("li");
        li.textContent = line;
        ul.appendChild(li);
      }
      // Mirror the legacy class contract — log-detail.spec.js asserts
      // `toHaveClass(/show/)` after the toggle click. We add the
      // class imperatively from the listener, but expose `id` so
      // spec assertions continue to anchor on the same selectors.
      return ul;
    };

    /** @type {HTMLElement[]} */
    const reveals = [];
    if (hasStack) {
      const stackEl = buildList("stacktrace", error.stacktrace);
      alert.appendChild(toggleBtn);
      alert.appendChild(stackEl);
      reveals.push(stackEl);
    } else {
      alert.appendChild(toggleBtn);
    }

    if (hasCause) {
      const causeWrapper = document.createElement("div");
      causeWrapper.id = "causeStacktrace";
      causeWrapper.style.display = "none";
      const causeHeading = document.createElement("h6");
      causeHeading.textContent = "Cause:";
      causeHeading.style.marginTop = "var(--space-3)";
      causeWrapper.appendChild(causeHeading);
      const causeUl = document.createElement("ul");
      causeUl.style.fontFamily = "var(--font-mono)";
      causeUl.style.fontSize = "var(--fs-12)";
      for (const line of error.cause_stacktrace) {
        const li = document.createElement("li");
        li.textContent = line;
        causeUl.appendChild(li);
      }
      causeWrapper.appendChild(causeUl);
      alert.appendChild(causeWrapper);
      reveals.push(causeWrapper);
    }

    toggleBtn.addEventListener("cts-click", () => {
      for (const el of reveals) {
        el.style.display = el.style.display === "none" ? "block" : "none";
        el.classList.add("show");
      }
      toggleBtn.style.display = "none";
    });
  }

  slot.appendChild(alert);
}
