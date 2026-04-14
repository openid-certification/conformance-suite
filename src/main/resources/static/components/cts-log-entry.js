import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";

const HTTP_BADGES = {
  request: { icon: "bi-arrow-right", label: "REQUEST" },
  response: { icon: "bi-arrow-left", label: "RESPONSE" },
  incoming: { icon: "bi-arrow-down", label: "INCOMING" },
  outgoing: { icon: "bi-arrow-up", label: "OUTGOING" },
  redirect: { icon: "bi-send-fill", label: "REDIRECT" },
  "redirect-in": { icon: "bi-arrow-down-circle", label: "REDIRECT-IN" },
};

class CtsLogEntry extends LitElement {
  static properties = {
    entry: { type: Object },
    _expanded: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this.entry = {};
    this._expanded = false;
  }

  _toggleMore() {
    this._expanded = !this._expanded;
  }

  _formatCurl() {
    const { more } = this.entry;
    if (!more) return "";
    const method = (more.method || "GET").toUpperCase();
    const url = more.url || "";
    const parts = [`curl -X ${method}`];
    if (more.headers) {
      for (const [key, value] of Object.entries(more.headers)) {
        parts.push(`-H '${key}: ${value}'`);
      }
    }
    if (more.body && typeof more.body === "string") {
      parts.push(`-d '${more.body}'`);
    } else if (more.body && typeof more.body === "object") {
      parts.push(`-d '${JSON.stringify(more.body)}'`);
    }
    parts.push(`'${url}'`);
    return parts.join(" \\\n  ");
  }

  async _copyCurl() {
    const curl = this._formatCurl();
    await navigator.clipboard.writeText(curl);
  }

  _renderHttpBadge() {
    const httpType = this.entry.http?.toLowerCase();
    const badge = HTTP_BADGES[httpType];
    if (!badge) return nothing;
    return html`
      <span class="badge bg-info"><span class="bi ${badge.icon}"></span> ${badge.label}</span>
      ${httpType === "request" ? html`
        <span class="badge bg-secondary" role="button" title="Copy as cURL" @click=${this._copyCurl} style="cursor: pointer;">
          <span class="bi bi-clipboard"></span> cURL
        </span>
      ` : nothing}
    `;
  }

  _renderRequirements() {
    const { requirements } = this.entry;
    if (!requirements || requirements.length === 0) return nothing;
    return html`
      <div class="labelCollection">
        ${requirements.map((req) => html`
          <span class="log-requirement badge bg-secondary">${req}</span>
        `)}
      </div>
    `;
  }

  _renderMoreButton() {
    const { more } = this.entry;
    if (!more || Object.keys(more).length === 0) return nothing;
    const count = Object.keys(more).length;
    const chevron = this._expanded ? "bi-chevron-up" : "bi-chevron-down";
    return html`
      <button class="btn btn-sm btn-light bg-gradient border border-secondary"
        @click=${this._toggleMore}>
        <span class="badge rounded-pill bg-secondary text-light">${count}</span>
        More <span class="bi ${chevron}" aria-hidden="true"></span>
      </button>
    `;
  }

  _renderMorePanel() {
    const { more } = this.entry;
    if (!this._expanded || !more) return nothing;
    return html`
      <div class="moreInfo mt-2">
        <dl class="row">
          ${Object.entries(more).map(([key, value]) => html`
            <dd class="col-sm-2 text-end text-break">${key}</dd>
            <dt class="col-sm-10 text-start wrapLongStrings">
              <pre class="mb-0">${typeof value === "string" ? value : JSON.stringify(value, null, 2)}</pre>
            </dt>
          `)}
        </dl>
      </div>
    `;
  }

  render() {
    const entry = this.entry;
    if (!entry || !entry._id) return nothing;

    const borderStyle = entry.blockId ? "border-left: 3px solid #336" : "";

    return html`
      <div class="row">
        <div class="col-md-12 logItem p-1" style=${borderStyle}>
          <div class="row">
            <div class="col-md-1">
              ${entry.time
                ? html`<small class="text-muted">${new Date(entry.time).toLocaleTimeString()}</small>`
                : nothing}
            </div>
            <div class="col-md-2 labelCollection">
              ${entry.result
                ? html`<cts-badge variant="${entry.result.toLowerCase()}" label="${entry.result}"></cts-badge>`
                : nothing}
              ${this._renderHttpBadge()}
              ${entry.upload
                ? html`<span class="badge bg-warning"><span class="bi bi-camera-fill"></span> IMAGE REQUIRED</span>`
                : nothing}
            </div>
            <div class="col-md-8">
              ${entry.src ? html`<small class="text-muted me-2">${entry.src}</small>` : nothing}
              ${entry.msg ? html`<span>${entry.msg}</span>` : nothing}
            </div>
            <div class="col-md-1">
              ${this._renderMoreButton()}
            </div>
          </div>
          <div class="row">
            <div class="col-md-1"></div>
            <div class="col-md-2">${this._renderRequirements()}</div>
            <div class="col-md-9">${this._renderMorePanel()}</div>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define("cts-log-entry", CtsLogEntry);
