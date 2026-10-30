import os
os.environ['JWT_SECRET_KEY'] = 'test'

from fastapi import FastAPI, Request
from fastapi.testclient import TestClient
from httpx import ASGITransport, AsyncClient
import asyncio

_app = FastAPI()

@_app.get("/ip")
async def get_ip(request: Request):
    return {"client": str(request.client)}

# Sync TestClient
tc = TestClient(_app)
r = tc.get("/ip")
print("TestClient IP:", r.json())

# Async httpx
async def check_async():
    transport = ASGITransport(app=_app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        r = await client.get("/ip")
        print("AsyncClient IP:", r.json())

asyncio.run(check_async())
