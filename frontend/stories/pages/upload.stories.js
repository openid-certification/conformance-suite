import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";

import "../../../src/main/resources/static/components/cts-image-upload.js";

export default {
  title: "Pages/Upload",
};

const PENDING_IMAGES = [
  {
    name: "screenshot-login",
    description: "Screenshot of the login page after authentication",
  },
  {
    name: "screenshot-consent",
    description: "Screenshot of the consent page",
  },
];

const EXISTING_IMAGES = [{ name: "screenshot-result", url: "images/placeholder.png" }];

export const Default = {
  render: () => html`
    <div class="container-fluid p-3">
      <h2 class="mb-3">Image Uploader</h2>
      <cts-image-upload
        test-id="test-abc-123"
        .pendingImages=${PENDING_IMAGES}
        .existingImages=${EXISTING_IMAGES}
      ></cts-image-upload>
    </div>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Page heading present
    await waitFor(() => {
      expect(canvas.getByText("Image Uploader")).toBeInTheDocument();
    });

    // Pending region: both pending blocks render
    const pendingBlocks = canvasElement.querySelectorAll('[data-testid="pending-image"]');
    expect(pendingBlocks.length).toBe(2);
    expect(
      canvas.getByText("Screenshot of the login page after authentication"),
    ).toBeInTheDocument();

    // Existing region: existing block renders
    const existingBlocks = canvasElement.querySelectorAll('[data-testid="existing-image"]');
    expect(existingBlocks.length).toBe(1);
    expect(canvas.getByText("screenshot-result")).toBeInTheDocument();

    // Component present
    expect(canvasElement.querySelector("cts-image-upload")).toBeTruthy();
  },
};
