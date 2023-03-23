
async function fetchJson(url) {
  const response = await fetch(url);
  if (!response.ok) {
    const message = `An error occurred: ${response.status}`;
    throw new Error(message);
  }
  const json = await response.json();
  return json;
}

async function loadPlans(options) {
    let plans = await fetchJson('/api/plan?' + new URLSearchParams({
        start: options.start,
        length: options.length,
        search: options.search,
        order: options.order,
        public: options.public
    }));
    return plans;
}

function planIdToAnchor(planId) {
    return `<a href="/plan-detail.html?plan=${planId}">${planId}</a>`;
}

function variantObjectToCellHtml(variant) {
    let html = '';
    Object.keys(variant).forEach(key => {
        html +=  `<div class="mb-1"><span class="text-muted d-block"><small>${key}</small></span>${variant[key]}</div>`;
    });
    return html;
}

function startedDateToCellHtml(started) {
    let startedDate = new Date(Date.parse(started));
    let localTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    let html = `<div class="mb-1"><span class="text-muted d-block"><small>ISO</small></span>${startedDate.toISOString()}</div>`
             + `<div class="mb-1"><span class="text-muted d-block"><small>${localTimeZone}</small></span>${startedDate.toLocaleString(undefined, { timeZone: localTimeZone })}</div>`;
    return html;
}

function queryStringToSearchOptions(queryString) {
    let urlParams = new URLSearchParams(queryString);
    return {
        start: urlParams.get('start') ?? 0,
        length: urlParams.get('length') ?? 10,
        search: urlParams.get('search') ?? '',
        order: urlParams.get('order') ?? 'started,desc',
        public: urlParams.get('public') ?? false,
    };
}

export {
	fetchJson,
	loadPlans,
	planIdToAnchor,
	variantObjectToCellHtml,
	startedDateToCellHtml,
	queryStringToSearchOptions
};
