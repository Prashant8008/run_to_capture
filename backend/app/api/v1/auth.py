from datetime import datetime, timedelta, timezone
import uuid
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update

from backend.app.database import get_db
from backend.app.config import settings
from backend.app.models.user import User, FactionEnum
from backend.app.models.session import UserSession
from backend.app.core.security import (
    get_password_hash,
    verify_password,
    create_access_token,
    create_refresh_token_string,
    verify_google_id_token
)
from backend.app.schemas.auth import (
    APIResponse,
    UserRegisterRequest,
    UserLoginRequest,
    GoogleAuthRequest,
    RefreshTokenRequest,
    LogoutRequest,
    TokenPairResponse,
    UserResponse
)
from backend.app.dependencies import get_current_user

router = APIRouter()

async def create_user_session(
    user: User,
    db: AsyncSession,
    request: Request
) -> TokenPairResponse:
    # Generate tokens
    access_token = create_access_token(subject=user.id)
    refresh_token = create_refresh_token_string()
    
    expires_at = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    
    # Store session in DB
    user_agent = request.headers.get("user-agent", "Unknown")[:255] if request else "Unknown"
    client_ip = request.client.host if request and request.client else "Unknown"
    
    session = UserSession(
        user_id=user.id,
        refresh_token=refresh_token,
        user_agent=user_agent,
        ip_address=client_ip,
        expires_at=expires_at.replace(tzinfo=None)
    )
    db.add(session)
    await db.commit()
    
    return TokenPairResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        user=UserResponse.model_validate(user)
    )


@router.post("/register", response_model=APIResponse[TokenPairResponse])
async def register_user(
    body: UserRegisterRequest,
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    # Check if email already exists
    stmt = select(User).where(User.email == body.email.lower().strip())
    existing = (await db.execute(stmt)).scalar_one_or_none()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={"success": False, "error": "Email already registered", "message": "An account with this email already exists."}
        )

    # Validate faction
    try:
        faction_val = FactionEnum(body.faction.upper()) if body.faction else FactionEnum.CIPHER
    except ValueError:
        faction_val = FactionEnum.CIPHER

    user = User(
        email=body.email.lower().strip(),
        hashed_password=get_password_hash(body.password),
        display_name=body.display_name.strip(),
        faction=faction_val,
        auth_provider="password",
        is_active=True,
        is_verified=False
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    token_data = await create_user_session(user, db, request)
    return APIResponse(success=True, data=token_data, message="Registration successful")


@router.post("/login", response_model=APIResponse[TokenPairResponse])
async def login_user(
    body: UserLoginRequest,
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.email == body.email.lower().strip())
    user = (await db.execute(stmt)).scalar_one_or_none()

    if not user or not user.hashed_password:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"success": False, "error": "Invalid credentials", "message": "Incorrect email or password."}
        )

    if not verify_password(body.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"success": False, "error": "Invalid credentials", "message": "Incorrect email or password."}
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail={"success": False, "error": "Account inactive", "message": "Your account has been deactivated."}
        )

    token_data = await create_user_session(user, db, request)
    return APIResponse(success=True, data=token_data, message="Login successful")


@router.post("/google", response_model=APIResponse[TokenPairResponse])
async def google_auth(
    body: GoogleAuthRequest,
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    # Verify Google token
    id_info = verify_google_id_token(body.id_token)
    
    # Extract identity
    if id_info:
        google_sub = id_info.get("sub")
        email = id_info.get("email")
        name = id_info.get("name") or body.display_name or "Runner"
        picture = id_info.get("picture")
    else:
        # Development fallback: If token is a test simulated token or development mock
        # We ensure it is structured and has an email/sub
        if body.id_token.startswith("dev_test_") or body.id_token.startswith("mock_"):
            google_sub = f"google_{body.id_token}"
            email = f"runner_{body.id_token[:8]}@example.com"
            name = body.display_name or "Tactical Runner"
            picture = None
        else:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"success": False, "error": "Invalid Google token", "message": "Google verification failed or token was cancelled."}
            )

    if not email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"success": False, "error": "Email missing from Google profile", "message": "Could not retrieve email."}
        )

    # Check if user exists by google_id or email
    stmt = select(User).where((User.google_id == google_sub) | (User.email == email.lower().strip()))
    user = (await db.execute(stmt)).scalar_one_or_none()

    if user:
        if not user.google_id:
            user.google_id = google_sub
        if picture and not user.avatar_url:
            user.avatar_url = picture
        await db.commit()
        await db.refresh(user)
    else:
        try:
            faction_val = FactionEnum(body.faction.upper()) if body.faction else FactionEnum.CIPHER
        except ValueError:
            faction_val = FactionEnum.CIPHER

        user = User(
            email=email.lower().strip(),
            display_name=name,
            google_id=google_sub,
            auth_provider="google",
            avatar_url=picture,
            faction=faction_val,
            is_active=True,
            is_verified=True
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)

    token_data = await create_user_session(user, db, request)
    return APIResponse(success=True, data=token_data, message="Google authentication successful")


@router.post("/refresh", response_model=APIResponse[TokenPairResponse])
async def refresh_token(
    body: RefreshTokenRequest,
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    stmt = select(UserSession).where(
        UserSession.refresh_token == body.refresh_token,
        UserSession.is_revoked == False
    )
    session = (await db.execute(stmt)).scalar_one_or_none()

    if not session or session.expires_at < datetime.utcnow():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"success": False, "error": "Invalid refresh token", "message": "Session expired or revoked. Please log in again."}
        )

    # Revoke old session and issue new rotation pair
    session.is_revoked = True
    session.last_used_at = datetime.utcnow()
    
    # Get user
    user_stmt = select(User).where(User.id == session.user_id)
    user = (await db.execute(user_stmt)).scalar_one_or_none()
    
    if not user or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"success": False, "error": "User not found or inactive", "message": "Invalid session user"}
        )

    token_data = await create_user_session(user, db, request)
    return APIResponse(success=True, data=token_data, message="Token refresh successful")


@router.post("/logout", response_model=APIResponse[dict])
async def logout_user(
    body: LogoutRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    if body.refresh_token:
        # Revoke specific session
        stmt = update(UserSession).where(
            UserSession.refresh_token == body.refresh_token,
            UserSession.user_id == current_user.id
        ).values(is_revoked=True)
        await db.execute(stmt)
    else:
        # Revoke all sessions for this user
        stmt = update(UserSession).where(
            UserSession.user_id == current_user.id
        ).values(is_revoked=True)
        await db.execute(stmt)
        
    await db.commit()
    return APIResponse(success=True, data={"logged_out": True}, message="Successfully logged out")
