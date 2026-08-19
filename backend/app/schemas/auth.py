from pydantic import BaseModel, EmailStr, Field
from typing import Optional, Generic, TypeVar
from datetime import datetime

T = TypeVar("T")

class APIResponse(BaseModel, Generic[T]):
    success: bool = True
    data: Optional[T] = None
    message: Optional[str] = None
    error: Optional[str] = None

class UserRegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8, description="Password must be at least 8 characters")
    display_name: str = Field(..., min_length=2, max_length=50)
    faction: Optional[str] = "CIPHER"

class UserLoginRequest(BaseModel):
    email: EmailStr
    password: str

class GoogleAuthRequest(BaseModel):
    id_token: str
    display_name: Optional[str] = None
    faction: Optional[str] = None

class RefreshTokenRequest(BaseModel):
    refresh_token: str

class LogoutRequest(BaseModel):
    refresh_token: Optional[str] = None

class TokenPairResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int # seconds
    user: "UserResponse"

class UserResponse(BaseModel):
    id: str
    email: str
    display_name: str
    faction: str
    avatar_url: Optional[str] = None
    auth_provider: str
    territory_color: str = "cyan"
    flag_config: Optional[dict] = None
    total_area_sq_meters: float
    total_distance_meters: float
    territories_count: int
    created_at: datetime

    class Config:
        from_attributes = True

TokenPairResponse.model_rebuild()
