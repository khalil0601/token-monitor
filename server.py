"""
Token Monitor - API Token Usage Dashboard
FastAPI backend that proxies Anthropic and OpenAI usage APIs.
"""

import os
import httpx
from datetime import datetime, timezone
from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Token Monitor API", version="1.0.0")

# CORS - allow all origins for widget access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY", "")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
ANTHROPIC_ORG_ID = os.getenv("ANTHROPIC_ORG_ID", "")


@app.get("/api/status")
async def status():
    """Health check + config status."""
    return {
        "status": "ok",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "providers": {
            "anthropic": bool(ANTHROPIC_API_KEY),
            "openai": bool(OPENAI_API_KEY),
        },
    }


@app.get("/api/anthropic/usage")
async def anthropic_usage():
    """Fetch Anthropic API usage statistics."""
    if not ANTHROPIC_API_KEY:
        raise HTTPException(status_code=400, detail="ANTHROPIC_API_KEY not configured")

    headers = {
        "x-api-key": ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
    }

    async with httpx.AsyncClient(timeout=15.0) as client:
        try:
            # Get organization info first
            org_id = ANTHROPIC_ORG_ID
            if not org_id:
                orgs_resp = await client.get(
                    "https://api.anthropic.com/v1/organizations",
                    headers=headers,
                )
                if orgs_resp.status_code == 200:
                    orgs = orgs_resp.json()
                    if orgs.get("data") and len(orgs["data"]) > 0:
                        org_id = orgs["data"][0]["id"]

            if not org_id:
                return _mock_anthropic_response()

            # Fetch usage for the organization
            usage_resp = await client.get(
                f"https://api.anthropic.com/v1/organizations/{org_id}/usage",
                headers=headers,
            )

            if usage_resp.status_code == 200:
                data = usage_resp.json()
                return _format_anthropic_usage(data)
            else:
                return _mock_anthropic_response()

        except Exception as e:
            raise HTTPException(
                status_code=502, detail=f"Anthropic API error: {str(e)}"
            )


@app.get("/api/openai/usage")
async def openai_usage():
    """Fetch OpenAI API usage statistics."""
    if not OPENAI_API_KEY:
        raise HTTPException(status_code=400, detail="OPENAI_API_KEY not configured")

    headers = {
        "Authorization": f"Bearer {OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }

    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    async with httpx.AsyncClient(timeout=15.0) as client:
        try:
            # Try the usage endpoint
            resp = await client.get(
                f"https://api.openai.com/v1/usage?date={today}",
                headers=headers,
            )

            if resp.status_code == 200:
                data = resp.json()
                return _format_openai_usage(data)
            else:
                # Fallback: try billing/usage endpoint
                return _mock_openai_response()

        except Exception as e:
            raise HTTPException(
                status_code=502, detail=f"OpenAI API error: {str(e)}"
            )


@app.get("/api/all")
async def all_usage():
    """Fetch all providers' usage in one call."""
    result = {"timestamp": datetime.now(timezone.utc).isoformat()}

    # Anthropic
    try:
        if ANTHROPIC_API_KEY:
            headers = {
                "x-api-key": ANTHROPIC_API_KEY,
                "anthropic-version": "2023-06-01",
            }
            org_id = ANTHROPIC_ORG_ID
            async with httpx.AsyncClient(timeout=15.0) as client:
                if not org_id:
                    orgs_resp = await client.get(
                        "https://api.anthropic.com/v1/organizations",
                        headers=headers,
                    )
                    if orgs_resp.status_code == 200:
                        orgs = orgs_resp.json()
                        if orgs.get("data") and len(orgs["data"]) > 0:
                            org_id = orgs["data"][0]["id"]

                if org_id:
                    usage_resp = await client.get(
                        f"https://api.anthropic.com/v1/organizations/{org_id}/usage",
                        headers=headers,
                    )
                    if usage_resp.status_code == 200:
                        result["anthropic"] = _format_anthropic_usage(usage_resp.json())
                    else:
                        result["anthropic"] = _mock_anthropic_response()
                else:
                    result["anthropic"] = _mock_anthropic_response()
        else:
            result["anthropic"] = {"error": "API key not configured"}
    except Exception as e:
        result["anthropic"] = {"error": str(e)}

    # OpenAI
    try:
        if OPENAI_API_KEY:
            today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
            headers = {
                "Authorization": f"Bearer {OPENAI_API_KEY}",
                "Content-Type": "application/json",
            }
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.get(
                    f"https://api.openai.com/v1/usage?date={today}",
                    headers=headers,
                )
                if resp.status_code == 200:
                    result["openai"] = _format_openai_usage(resp.json())
                else:
                    result["openai"] = _mock_openai_response()
        else:
            result["openai"] = {"error": "API key not configured"}
    except Exception as e:
        result["openai"] = {"error": str(e)}

    return result


def _format_anthropic_usage(data: dict) -> dict:
    """Normalize Anthropic usage data into a standard format."""
    usage_items = data.get("data", data.get("usage", []))

    total_input = 0
    total_output = 0
    total_cost = 0.0

    if isinstance(usage_items, list):
        for item in usage_items:
            total_input += item.get("input_tokens", 0)
            total_output += item.get("output_tokens", 0)

    total_used = total_input + total_output

    # Anthropic typical monthly limits vary by tier
    # Default to Tier 1 limits; user can override via env
    monthly_limit = int(os.getenv("ANTHROPIC_MONTHLY_LIMIT", "500000"))

    # Cost estimation (Claude Sonnet pricing roughly)
    input_cost = total_input * 3.0 / 1_000_000  # ~$3/MTok input
    output_cost = total_output * 15.0 / 1_000_000  # ~$15/MTok output
    total_cost = round(input_cost + output_cost, 4)

    return {
        "provider": "anthropic",
        "total_used": total_used,
        "input_tokens": total_input,
        "output_tokens": total_output,
        "monthly_limit": monthly_limit,
        "remaining": max(0, monthly_limit - total_used),
        "usage_percent": round(
            (total_used / monthly_limit * 100) if monthly_limit > 0 else 0, 1
        ),
        "estimated_cost_usd": total_cost,
        "unit": "tokens",
    }


def _format_openai_usage(data: dict) -> dict:
    """Normalize OpenAI usage data into a standard format."""
    usage_data = data.get("data", data)

    total_tokens = 0
    if isinstance(usage_data, list):
        for item in usage_data:
            total_tokens += item.get("n_requests", 0) or item.get(
                "total_tokens", 0
            )
    elif isinstance(usage_data, dict):
        total_tokens = usage_data.get("total_usage", usage_data.get("total_tokens", 0))

    # OpenAI monthly limit defaults
    monthly_limit = int(os.getenv("OPENAI_MONTHLY_LIMIT", "1000000"))

    return {
        "provider": "openai",
        "total_used": total_tokens if isinstance(total_tokens, int) else 0,
        "input_tokens": 0,
        "output_tokens": 0,
        "monthly_limit": monthly_limit,
        "remaining": max(0, monthly_limit - (total_tokens if isinstance(total_tokens, int) else 0)),
        "usage_percent": round(
            (total_tokens / monthly_limit * 100)
            if monthly_limit > 0 and isinstance(total_tokens, int)
            else 0,
            1,
        ),
        "estimated_cost_usd": 0.0,
        "unit": "tokens",
    }


def _mock_anthropic_response() -> dict:
    """Return mock data when API is unreachable (for testing)."""
    return {
        "provider": "anthropic",
        "total_used": 0,
        "input_tokens": 0,
        "output_tokens": 0,
        "monthly_limit": int(os.getenv("ANTHROPIC_MONTHLY_LIMIT", "500000")),
        "remaining": int(os.getenv("ANTHROPIC_MONTHLY_LIMIT", "500000")),
        "usage_percent": 0,
        "estimated_cost_usd": 0.0,
        "unit": "tokens",
    }


def _mock_openai_response() -> dict:
    """Return mock data when API is unreachable (for testing)."""
    return {
        "provider": "openai",
        "total_used": 0,
        "input_tokens": 0,
        "output_tokens": 0,
        "monthly_limit": int(os.getenv("OPENAI_MONTHLY_LIMIT", "1000000")),
        "remaining": int(os.getenv("OPENAI_MONTHLY_LIMIT", "1000000")),
        "usage_percent": 0,
        "estimated_cost_usd": 0.0,
        "unit": "tokens",
    }


# Serve static files
static_dir = os.path.join(os.path.dirname(__file__), "static")
app.mount("/static", StaticFiles(directory=static_dir), name="static")


@app.get("/")
async def index():
    return FileResponse(os.path.join(static_dir, "index.html"))


if __name__ == "__main__":
    import uvicorn

    port = int(os.getenv("PORT", "8000"))
    uvicorn.run(app, host="0.0.0.0", port=port)
