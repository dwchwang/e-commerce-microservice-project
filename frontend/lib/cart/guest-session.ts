"use client";

import Cookies from "js-cookie";
import { v4 as uuidv4 } from "uuid";

const COOKIE_NAME = "guest_session_id";

export function getOrCreateGuestSessionId(): string {
  let id = Cookies.get(COOKIE_NAME);
  if (!id) {
    id = uuidv4();
    Cookies.set(COOKIE_NAME, id, {
      expires: 30,
      sameSite: "lax",
      path: "/",
    });
  }
  return id;
}

export function getGuestSessionId(): string | undefined {
  return Cookies.get(COOKIE_NAME);
}
