// catalog-ramp.js — Bai ramp bac thang 0 -> 2000 VU tren read path (catalog/search/reviews).
//
// Muc dich: do diem bao hoa cua he thong tren EC2 16 GB, lam so lieu nang cap Muc 5.9 cua bao cao.
// Danh vao read path PUBLIC (khong bi rate limiter) de VU cao tao tai that thay vi loat 429.
//
// Cach chay (tu thu muc goc repo):
//   BASE_URL="http://13.213.118.96:8080" \
//     k6 run --out json=.test/results/catalog-ramp-$(date +%Y%m%d-%H%M).json \
//     .test/load/catalog-ramp.js | tee .test/results/catalog-ramp-$(date +%Y%m%d-%H%M).txt
//
//   # Smoke (~1.5 phut) truoc khi chay that:
//   BASE_URL="http://13.213.118.96:8080" RAMP_PROFILE=smoke k6 run .test/load/catalog-ramp.js
//
//   # Bieu do time-series (duong cong bao hoa) cho figure k6:
//   K6_WEB_DASHBOARD=true \
//     K6_WEB_DASHBOARD_EXPORT=.test/results/catalog-ramp-dashboard.html \
//     BASE_URL="http://13.213.118.96:8080" k6 run .test/load/catalog-ramp.js
//
// Tham so qua __ENV:
//   BASE_URL        (mac dinh http://localhost:8080)
//   RAMP_PROFILE    full | smoke           (mac dinh full)
//   PEAK_VUS        dinh VU cao nhat        (mac dinh 2000, smoke 30)
//   STAGE_DURATION  thoi luong moi bac      (mac dinh 2m, smoke 10s)
//   THINK_MIN/MAX   think time giua request (giay)

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { report } from './lib/summary.js';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const PROFILE = (__ENV.RAMP_PROFILE || 'full').toLowerCase();
const IS_SMOKE = PROFILE === 'smoke';

const PEAK_VUS = Number(__ENV.PEAK_VUS || (IS_SMOKE ? 30 : 1500));
const STAGE = __ENV.STAGE_DURATION || (IS_SMOKE ? '10s' : '2m');
const THINK_MIN = Number(__ENV.THINK_MIN || 0.5);
const THINK_MAX = Number(__ENV.THINK_MAX || 1.5);

// Cac bac thang theo ti le cua PEAK: 10% -> 30% -> 60% -> 100%.
// Moi bac gom 1 doan ramp len + 1 doan giu de Prometheus scrape on dinh, cuoi cung ramp ve 0.
const FRACTIONS = [0.1, 0.3, 0.6, 1.0];

function buildStages() {
  const stages = [];
  for (const f of FRACTIONS) {
    const target = Math.max(1, Math.round(f * PEAK_VUS));
    stages.push({ duration: STAGE, target }); // ramp len bac nay
    stages.push({ duration: STAGE, target }); // giu o bac nay
  }
  stages.push({ duration: STAGE, target: 0 }); // ramp down
  return stages;
}

const PRODUCTS = new SharedArray('products', () => JSON.parse(open('../seed/products.json')));

export const options = {
  scenarios: {
    catalog_ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: buildStages(),
      gracefulRampDown: '10s',
      tags: { scenario: 'catalog-ramp' },
    },
  },
  // Threshold noi long: muc tieu la DO diem gay, khong phai pass/fail gat gao.
  // Khong dat abortOnFail de duong cong xau di van duoc ghi lai het.
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000', 'p(99)<5000'],
    // Tach p95 theo tung loai request -> build-k6-report.mjs dung de phan tich service nao nong.
    'http_req_duration{name:list}': ['p(95)<3000'],
    'http_req_duration{name:detail}': ['p(95)<3000'],
    'http_req_duration{name:search}': ['p(95)<3000'],
    'http_req_duration{name:reviews}': ['p(95)<3000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

function think() {
  sleep(THINK_MIN + Math.random() * (THINK_MAX - THINK_MIN));
}

export default function () {
  // 1) Danh sach san pham (path nang nhat, dung Redis cache + Postgres).
  const list = http.get(`${BASE}/api/products?size=12&page=0`, { tags: { name: 'list' } });
  check(list, { 'list 200': (r) => r.status === 200 });
  think();

  // 2) Chi tiet mot san pham ngau nhien.
  const product = PRODUCTS[Math.floor(Math.random() * PRODUCTS.length)];
  const detail = http.get(`${BASE}/api/products/${product.id}`, { tags: { name: 'detail' } });
  check(detail, { 'detail 200': (r) => r.status === 200 });
  think();

  // 3) Search (Elasticsearch read model).
  const search = http.get(`${BASE}/api/search?q=laptop`, { tags: { name: 'search' } });
  check(search, { 'search not 5xx': (r) => r.status < 500 });
  think();

  // 4) Review cua san pham.
  const reviews = http.get(`${BASE}/api/reviews/product/${product.id}?size=5`, { tags: { name: 'reviews' } });
  check(reviews, { 'reviews not 5xx': (r) => r.status < 500 });
  think();
}

export function handleSummary(data) {
  return report(data, {
    name: 'catalog-ramp',
    label: `Catalog ramp 0->${PEAK_VUS} VU (saturation)`,
    kind: 'ramp',
    maxVU: PEAK_VUS,
    notes: `Read path public, bac thang ${FRACTIONS.map((f) => Math.round(f * PEAK_VUS)).join('/')} VU, profile=${PROFILE}`,
  });
}
