const $ = document.querySelector.bind(document);
const $$ = document.querySelectorAll.bind(document);

async function fetchJson(url) {
  const response = await fetch(url);
  if (!response.ok) {
    const message = `An error occurred: ${response.status}`;
    throw new Error(message);
  }
  const json = await response.json();
  return json;
}

(async () => {
    try {
        const userInfo = await fetchJson('/api/currentuser');
        $('#logged-in-user').textContent = userInfo.displayName;
        if (userInfo.isAdmin) {
        	$('#is-admin').classList.remove('visually-hidden');
        }
    } catch { }

    try {
        const serverInfo = await fetchJson('/api/server');
        $('#suite-version').textContent = ` version ${serverInfo.version}. `
        if (serverInfo.external_ip) {
            $('#external-ip').textContent = `External IP: ${serverInfo.external_ip}.`
        }
    } catch { }
})();
