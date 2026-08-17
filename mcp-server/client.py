"""HTTP client for the Squadron API. Handles auth and token refresh."""
import os
import httpx

BASE_URL = os.getenv("SQUADRON_API_URL", "http://localhost:8080")
_EMAIL = os.getenv("SQUADRON_EMAIL", "admin@squadron.local")
_PASSWORD = os.getenv("SQUADRON_PASSWORD", "admin123")


class ApiClient:
    def __init__(self):
        self._token: str | None = None
        self._role: str | None = None

    async def _login(self) -> None:
        async with httpx.AsyncClient() as c:
            res = await c.post(
                f"{BASE_URL}/api/auth/login",
                json={"email": _EMAIL, "password": _PASSWORD},
            )
            res.raise_for_status()
            data = res.json()
            self._token = data["token"]
            self._role = data["role"]

    def _headers(self) -> dict:
        return {"Authorization": f"Bearer {self._token}"}

    async def _request(self, method: str, path: str, **kwargs):
        if not self._token:
            await self._login()
        async with httpx.AsyncClient() as c:
            res = await c.request(
                method, f"{BASE_URL}{path}", headers=self._headers(), **kwargs
            )
            if res.status_code == 401:
                await self._login()
                res = await c.request(
                    method, f"{BASE_URL}{path}", headers=self._headers(), **kwargs
                )
            res.raise_for_status()
            if res.status_code == 204:
                return None
            return res.json()

    async def get(self, path: str, params: dict | None = None):
        return await self._request("GET", path, params=params)

    async def post(self, path: str, data: dict):
        return await self._request("POST", path, json=data)

    async def put(self, path: str, data: dict):
        return await self._request("PUT", path, json=data)

    async def delete(self, path: str):
        return await self._request("DELETE", path)

    @property
    def is_admin(self) -> bool:
        return self._role == "ADMIN"
