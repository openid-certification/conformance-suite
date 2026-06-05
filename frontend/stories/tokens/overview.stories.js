import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";

export default {
  title: "Tokens/Overview",
};

export const Placeholder = {
  render: () => html`
    <h2>Design Tokens</h2>
    <p>
      Token documentation (color, typography, spacing) lands here in a follow-up. The token sheet
      (<code>css/oidf-tokens.css</code>) is already loaded into every story canvas, so token stories
      can read live custom-property values directly.
    </p>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Design Tokens")).toBeInTheDocument();
    });
  },
};
