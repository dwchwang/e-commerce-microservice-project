import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { login } from './lib/auth.js';
import { report } from './lib/summary.js';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const RAMP_UP = __ENV.CHECKOUT_RAMP_UP || '1m';
const HOLD = __ENV.CHECKOUT_HOLD || '5m';
const RAMP_DOWN = __ENV.CHECKOUT_RAMP_DOWN || '1m';
const TARGET_VUS = Number(__ENV.CHECKOUT_TARGET_VUS || 150);
const users = new SharedArray('users', () => JSON.parse(open('../seed/users.json')));
const products = new SharedArray('products', () => JSON.parse(open('../seed/products.json')));
let cachedToken;
let tokenExpiresAt = 0;

export const options = {
  stages: [
    { duration: RAMP_UP, target: TARGET_VUS },
    { duration: HOLD, target: TARGET_VUS },
    { duration: RAMP_DOWN, target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    'checks{type:order_created}': ['rate>0.95'],
  },
};

export default function () {
  const tk = tokenForVu();
  const prod = products[(__VU - 1) % products.length];
  const headers = {
    'Authorization': `Bearer ${tk}`,
    'Content-Type': 'application/json',
  };

  const cart = http.post(`${BASE}/api/cart/items`, JSON.stringify({
    productId: prod.id, quantity: 1,
  }), { headers });
  check(cart, { 'cart add ok': (r) => r.status === 200 || r.status === 201 });
  sleep(0.5);

  const order = http.post(`${BASE}/api/orders`, JSON.stringify({
    paymentMethod: 'COD',
    shippingName: 'Load Test',
    shippingPhone: '0900000000',
    shippingAddress: '1 Test St, HCM',
    items: [{ productId: prod.id, quantity: 1 }],
  }), { headers });

  check(order, { 'order_created': (r) => r.status === 201 || r.status === 200 }, { type: 'order_created' });
  sleep(2);
}

function tokenForVu() {
  const now = Date.now();
  if (!cachedToken || now >= tokenExpiresAt) {
    const user = users[(__VU - 1) % users.length];
    cachedToken = login(user.email, user.password);
    tokenExpiresAt = now + 240000;
  }
  return cachedToken;
}

export function handleSummary(data) {
  return report(data, {
    name: 'checkout-stress',
    label: `Checkout stress ${TARGET_VUS} VU`,
    kind: 'stress',
    maxVU: TARGET_VUS,
    notes: 'Luong Saga dat hang COD: cart -> order',
  });
}
