import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';
import { report } from './lib/summary.js';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const FLASH_SALE_ID = __ENV.FLASH_SALE_ID;
const EXPECTED_SUCCESS = Number(__ENV.EXPECTED_SUCCESS || 100);
const SPIKE_TARGET_VUS = Number(__ENV.FLASH_SALE_TARGET_VUS || 1500);
const RAMP_UP = __ENV.FLASH_SALE_RAMP_UP || '15s';
const HOLD = __ENV.FLASH_SALE_HOLD || '60s';
const RAMP_DOWN = __ENV.FLASH_SALE_RAMP_DOWN || '15s';
const tokens = new SharedArray('tokens', () => JSON.parse(open('../seed/tokens.json')));

const purchaseSuccesses = new Counter('flash_sale_purchase_successes');
const soldOutResponses = new Counter('flash_sale_sold_out_responses');

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP_UP, target: SPIKE_TARGET_VUS },
        { duration: HOLD, target: SPIKE_TARGET_VUS },
        { duration: RAMP_DOWN, target: 0 },
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    flash_sale_purchase_successes: [`count==${EXPECTED_SUCCESS}`],
    flash_sale_sold_out_responses: ['count>0'],
  },
};

export default function () {
  const tk = tokenForVu();
  const res = http.post(`${BASE}/api/flash-sales/${FLASH_SALE_ID}/purchase`,
    JSON.stringify({
      paymentMethod: 'COD',
      shippingName: 'Load Test',
      shippingPhone: '0900000000',
      shippingAddress: '1 Test St, HCM',
    }), {
      headers: {
        'Authorization': `Bearer ${tk}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'flash_sale_buy' },
    });

  const success = res.status === 200 || res.status === 201;
  const soldOut = res.status === 400 && res.body.includes('Sold out');

  if (success) {
    purchaseSuccesses.add(1);
  }
  if (soldOut) {
    soldOutResponses.add(1);
  }

  check(res, {
    'success': (r) => r.status === 200 || r.status === 201,
  }, { outcome: 'success' });
  check(res, {
    'sold_out': (r) => r.status === 400 && r.body.includes('Sold out'),
  }, { outcome: 'sold_out' });
  check(res, {
    'duplicate': (r) => r.status === 400 && r.body.includes('already purchased'),
  }, { outcome: 'duplicate' });
  check(res, {
    'rate_limit': (r) => r.status === 429,
  }, { outcome: 'rate_limit' });

  sleep(0.1);
}

function tokenForVu() {
  return tokens[(__VU - 1) % tokens.length];
}

export function handleSummary(data) {
  return report(data, {
    name: 'flash-sale-spike',
    label: `Flash-sale spike ${SPIKE_TARGET_VUS} VU`,
    kind: 'spike',
    maxVU: SPIKE_TARGET_VUS,
    notes: `${SPIKE_TARGET_VUS} VU tranh mua ${EXPECTED_SUCCESS} slot`,
  });
}
