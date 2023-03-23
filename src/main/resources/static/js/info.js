async function setUserInfo() {
	const response = await fetch('/api/currentuser');
	const userInfo = await response.json();
	document.querySelector('#logged-in-user').textContent = userInfo.displayName;
	if (userInfo.isAdmin) {
		document.querySelector('#is-admin').classList.remove('visually-hidden');
	}
}

async function setServerInfo() {
	const response = await fetch('/api/server');
	const serverInfo = await response.json();
	document.querySelector('#suite-version').textContent = ` version ${serverInfo.version}. `
	if (serverInfo.external_ip) {
		document.querySelector('#external-ip').textContent = `External IP: ${serverInfo.external_ip}.`
	}
}

(async () => {
	setUserInfo();
	setServerInfo();
})();
