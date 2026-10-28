// k6 read scenario (T13): the public village list under load — paging, sorting, filtering and text search.
//
// Run it (the app on port 8080, the database seeded with loadtest/seed/seed-villages.sql):
//   docker run --rm -i -v "$PWD/loadtest:/loadtest" grafana/k6:1.0.0 run /loadtest/k6/read-villages.js
//
// Environment variables (pass with -e NAME=value after "run"):
//   BASE_URL  default http://host.docker.internal:8080 (the host machine as seen from the k6 container)
//   VUS       peak number of virtual users, default 20
//   DURATION  how long to hold the peak, default 1m

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VUS = parseInt(__ENV.VUS || '20', 10);
const DURATION = __ENV.DURATION || '1m';

export const options = {
    stages: [
        { duration: '15s', target: VUS }, // ramp up
        { duration: DURATION, target: VUS }, // hold the peak: the numbers in the report come from here
        { duration: '10s', target: 0 }, // ramp down
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        // Per request type, so a slow search cannot hide behind fast plain listing
        'http_req_duration{name:list}': ['p(95)<500'],
        'http_req_duration{name:search}': ['p(95)<500'],
        'http_req_duration{name:filter}': ['p(95)<500'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

// Words that appear in the seeded village names and descriptions, plus one that matches nothing.
// A miss is the worst case for an unindexed LIKE '%...%' search: every row must be checked.
const SEARCH_TERMS = ['hanina', 'surif', 'battir', 'yatta', 'burqa', 'olive', 'spring', 'qadima', 'beit', 'nomatchxyz'];
const SORTS = ['id,asc', 'elevationM,desc', 'latitude,asc', 'locationDescription,asc'];

function pick(list) {
    return list[Math.floor(Math.random() * list.length)];
}

function get(path, name) {
    const res = http.get(`${BASE_URL}${path}`, { tags: { name } });
    check(res, {
        [`${name}: status 200`]: (r) => r.status === 200,
        [`${name}: has items`]: (r) => r.status === 200 && r.json('items') !== undefined,
    });
    return res;
}

export function setup() {
    // Fail fast with a clear message if the app is not running or the seed is missing
    const res = http.get(`${BASE_URL}/api/v1/villages?size=1`);
    if (res.status !== 200) {
        throw new Error(`App not reachable at ${BASE_URL} (status ${res.status}). Is it running?`);
    }
    const total = res.json('totalItems');
    if (total < 1000) {
        console.warn(`Only ${total} villages: seed the database first, or the results will look too fast.`);
    }
    return { totalItems: total };
}

export default function (data) {
    // A mix that roughly matches how people browse: mostly listing and searching, some filtering
    const roll = Math.random();
    const lastPage = Math.max(0, Math.floor(data.totalItems / 20) - 1);

    if (roll < 0.4) {
        const page = Math.floor(Math.random() * Math.min(lastPage + 1, 50));
        get(`/api/v1/villages?page=${page}&size=20&sort=${pick(SORTS)}`, 'list');
    } else if (roll < 0.8) {
        get(`/api/v1/villages?q=${pick(SEARCH_TERMS)}&size=20`, 'search');
    } else {
        get(`/api/v1/villages?governorateId=1&size=20&sort=${pick(SORTS)}`, 'filter');
    }

    sleep(Math.random() * 0.5 + 0.25); // think time between requests: 0.25–0.75 s
}
