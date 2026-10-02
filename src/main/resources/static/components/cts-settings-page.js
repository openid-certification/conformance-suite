import { LitElement, html } from "lit";
import "./cts-settings-cmf-chile.js";

/**
 * The server settings page. Each settings section is its own component that
 * loads and saves its section of `/api/admin/settings/<section>`, and shows
 * its own admin-only alert from that endpoint's 403. A new section is a
 * `cts-settings-<section>` component listed here.
 * @property {undefined} [noAttributes] - This component has no public
 *   attributes or properties; it only lists the section components.
 */
class CtsSettingsPage extends LitElement {
  createRenderRoot() {
    return this;
  }

  render() {
    return html`<cts-settings-cmf-chile></cts-settings-cmf-chile>`;
  }
}

customElements.define("cts-settings-page", CtsSettingsPage);
