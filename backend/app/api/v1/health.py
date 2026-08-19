from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()

class HealthStatus(BaseModel):
    status: str

class HealthResponse(BaseModel):
    success: bool
    data: HealthStatus

@router.get("/health", response_model=HealthResponse)
async def get_health():
    """
    Health check endpoint for Run2Capture service verification.
    """
    return HealthResponse(
        success=True,
        data=HealthStatus(status="ok")
    )
