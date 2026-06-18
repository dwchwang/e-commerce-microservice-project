import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { report } from './lib/summary.js';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const RAMP_UP = __ENV.CATALOG_RAMP_UP || '1m';
const HOLD = __ENV.CATALOG_HOLD || '5m';
const RAMP_DOWN = __ENV.CATALOG_RAMP_DOWN || '1m';
const TARGET_VUS = Number(__ENV.CATALOG_TARGET_VUS || 300);
const RAMP_VUS = Number(__ENV.CATALOG_RAMP_VUS || Math.min(150, TARGET_VUS));

const PRODUCTS = new SharedArray('products', () => JSON.parse(open('../seed/products.json')));

export const options = {
  stages: [
    { duration: RAMP_UP, target: RAMP_VUS },
    { duration: HOLD, target: TARGET_VUS },
    { duration: RAMP_DOWN, target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const list = http.get(`${BASE}/api/products?size=12&page=0`, { tags: { name: 'list' } });
  check(list, { 'list 200': (r) => r.status === 200 });
  sleep(1);

  const product = PRODUCTS[Math.floor(Math.random() * PRODUCTS.length)];
  const detail = http.get(`${BASE}/api/products/${product.id}`, { tags: { name: 'detail' } });
  check(detail, { 'detail 200': (r) => r.status === 200 });
  sleep(1);

  const search = http.get(`${BASE}/api/search?q=laptop`, { tags: { name: 'search' } });
  check(search, { 'search not 5xx': (r) => r.status < 500 });
  sleep(1);

  const reviews = http.get(`${BASE}/api/reviews/product/${product.id}?size=5`, { tags: { name: 'reviews' } });
  check(reviews, { 'reviews not 5xx': (r) => r.status < 500 });
  sleep(2);
}

export function handleSummary(data) {
  return report(data, {
    name: 'catalog-soak',
    label: `Catalog baseline ${TARGET_VUS} VU`,
    kind: 'baseline',
    maxVU: TARGET_VUS,
    notes: 'Read path: list/detail/search/reviews (vung khoe, duoi knee ~300 VU)',
  });
}
