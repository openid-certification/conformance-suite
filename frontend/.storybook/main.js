/** @type {import('@storybook/web-components-vite').StorybookConfig} */
const config = {
  stories: ["../stories/**/*.stories.js"],
  addons: ["@storybook/addon-essentials"],
  framework: "@storybook/web-components-vite",
  staticDirs: [{ from: "../../src/main/resources/static", to: "/" }],
};

export default config;
