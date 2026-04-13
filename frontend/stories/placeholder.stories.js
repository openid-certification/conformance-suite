import { html } from "lit";

export default {
  title: "Setup/Placeholder",
};

export const BootstrapWorks = {
  render: () => html`
    <div class="container-fluid">
      <div class="pageHeader">
        <div class="row">
          <div class="col-md-7">
            <a href="index.html"><img src="/images/openid.png" alt="OpenID logo" /></a>
          </div>
          <div class="col-md-5 text-end">
            <span class="badge bg-success">Bootstrap is working</span>
          </div>
        </div>
      </div>
    </div>
  `,
};
