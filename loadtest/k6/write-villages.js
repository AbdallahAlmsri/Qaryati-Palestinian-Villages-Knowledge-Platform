// k6 write scenario (T13): logged-in contributors adding data under load.
// setup() registers one user and logs in once; every virtual user then reuses that token,
// so the login rate limit (10 per minute per IP) is never hit and only the writes are measured.
//
// Run it (the app on port 8080):
//   docker run --rm -i -v "$PWD/loadtest:/loadtest" grafana/k6:1.0.0 run /loadtest/k6/write-villages.js
//
// Environment variables (pass with -e NAME=value after "run"):
//   BASE_URL        default http://host.docker.internal:8080
//   VUS             peak number of virtual users, default 10
//   DURATION        how long to hold the peak, default 1m
//   GOVERNORATE_ID  governorate for the new villages, default 1 (Nablus, always present)
//
// Every village created here ends with "(load test data)", so loadtest/seed/remove-seed-villages.sql removes it.

import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VUS = parseInt(__ENV.VUS || '10', 10);
const DURATION = __ENV.DURATION || '1m';
const GOVERNORATE_ID = parseInt(__ENV.GOVERNORATE_ID || '1', 10);

export const options = {
    stages: [
        { duration: '15s', target: VUS },
        { duration: DURATION, target: VUS },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        'http_req_duration{name:create_village}': ['p(95)<800'],
        'http_req_duration{name:add_name}': ['p(95)<800'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export function setup() {
    // A fresh user per run, so runs never collide on usernames
    const username = `k6_writer_${Date.now()}`;
    const password = 'loadtest-password-123';

    const registered = http.post(`${BASE_URL}/api/v1/auth/register`,
        JSON.stringify({ username, email: `${username}@example.com`, password }),
        { headers: JSON_HEADERS, tags: { name: 'setup' } });
    if (registered.status !== 200) {
        fail(`Register failed with status ${registered.status}: ${registered.body}`);
    }

    // Log in once, as the handoff asks: the token is shared by every virtual user for the whole run
    const loggedIn = http.post(`${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ username, password }),
        { headers: JSON_HEADERS, tags: { name: 'setup' } });
    if (loggedIn.status !== 200) {
        fail(`Login failed with status ${loggedIn.status}: ${loggedIn.body}`);
    }

    return { token: loggedIn.json('token') };
}

function authHeaders(token) {
    return { ...JSON_HEADERS, Authorization: `Bearer ${token}` };
}

function randomBetween(min, max) {
    return min + Math.random() * (max - min);
}

export default function (data) {
    // 1. Create a village (every iteration)
    const village = {
        governorateId: GOVERNORATE_ID,
        latitude: Number(randomBetween(31.25, 32.55).toFixed(6)),
        longitude: Number(randomBetween(34.25, 35.6).toFixed(6)),
        locationDescription: `k6 village vu${__VU}-it${__ITER} (load test data)`,
        elevationM: Math.floor(randomBetween(200, 1000)),
    };
    const created = http.post(`${BASE_URL}/api/v1/villages`, JSON.stringify(village),
        { headers: authHeaders(data.token), tags: { name: 'create_village' } });
    const ok = check(created, {
        'create_village: status 201': (r) => r.status === 201,
        'create_village: returns id': (r) => r.status === 201 && r.json('id') !== undefined,
    });

    // 2. Half the time, also add a name to the new village: a second write path with its own validation
    if (ok && Math.random() < 0.5) {
        const name = {
            name: `K6 Village ${__VU}-${__ITER}`,
            nameType: 'current',
            language: 'en',
            source: 'k6 load test',
        };
        const added = http.post(`${BASE_URL}/api/v1/villages/${created.json('id')}/names`, JSON.stringify(name),
            { headers: authHeaders(data.token), tags: { name: 'add_name' } });
        check(added, { 'add_name: status 201': (r) => r.status === 201 });
    }

    sleep(Math.random() * 0.5 + 0.5); // writers are slower than readers: 0.5–1 s between actions
}
