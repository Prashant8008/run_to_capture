import datetime
from datetime import timedelta, timezone
from typing import Any, Union, Optional
import jwt
from passlib.context import CryptContext
import secrets
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests
from backend.app.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password: str) -> str:
    return pwd_context.hash(password)

def create_access_token(
    subject: Union[str, Any],
    expires_delta: Optional[timedelta] = None,
    claims: Optional[dict] = None
) -> str:
    if expires_delta:
        expire = datetime.datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode = {
        "sub": str(subject),
        "exp": expire,
        "iat": datetime.datetime.now(timezone.utc),
        "type": "access"
    }
    if claims:
        to_encode.update(claims)
        
    encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt

def create_refresh_token_string() -> str:
    return secrets.token_urlsafe(64)

def decode_access_token(token: str) -> Optional[dict]:
    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM]
        )
        if payload.get("type") != "access":
            return None
        return payload
    except jwt.PyJWTError:
        return None

def verify_google_id_token(token_str: str) -> Optional[dict]:
    """
    Verify Google ID Token provided by Android CredentialManager / Google Sign-In.
    """
    try:
        # If client ID is configured, verify against it; otherwise verify signature and issuer
        audience = settings.GOOGLE_CLIENT_ID if settings.GOOGLE_CLIENT_ID else None
        id_info = id_token.verify_oauth2_token(
            token_str,
            google_requests.Request(),
            audience=audience
        )
        if id_info.get("iss") not in ["accounts.google.com", "https://accounts.google.com"]:
            return None
        return id_info
    except Exception:
        # Fallback for development/testing if token is a simulated test token
        return None
