from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from backend.app.database import get_db
from backend.app.models.user import User
from backend.app.schemas.auth import APIResponse, UserResponse
from backend.app.schemas.customization import (
    CustomizationUpdateRequest,
    CustomizationResponse,
    CustomizationOptionsResponse,
    FlagConfig,
    STANDARD_TERRITORY_COLORS,
    ALLOWED_FLAG_BACKGROUNDS,
    ALLOWED_FLAG_PATTERNS,
    ALLOWED_FLAG_EMBLEMS,
    ALLOWED_FLAG_BORDERS,
    validate_map_contrast
)
from backend.app.dependencies import get_current_user

router = APIRouter()

@router.get("/me", response_model=APIResponse[UserResponse])
async def get_current_user_profile(
    current_user: User = Depends(get_current_user)
):
    """
    Get profile of the currently authenticated player/user.
    Never trusts client-supplied IDs. Extracts identity directly from verified JWT token.
    """
    return APIResponse(
        success=True,
        data=UserResponse.model_validate(current_user),
        message="Profile fetched successfully"
    )


@router.get("/customization/options", response_model=APIResponse[CustomizationOptionsResponse])
async def get_customization_options():
    """
    Return all allowed standard territory colors, backgrounds, patterns, emblems, and borders.
    """
    options = CustomizationOptionsResponse(
        standard_colors=STANDARD_TERRITORY_COLORS,
        backgrounds=sorted(list(ALLOWED_FLAG_BACKGROUNDS)),
        patterns=sorted(list(ALLOWED_FLAG_PATTERNS)),
        emblems=sorted(list(ALLOWED_FLAG_EMBLEMS)),
        borders=sorted(list(ALLOWED_FLAG_BORDERS))
    )
    return APIResponse(
        success=True,
        data=options,
        message="Customization options retrieved successfully"
    )


@router.get("/users/me/customization", response_model=APIResponse[CustomizationResponse])
async def get_user_customization(
    current_user: User = Depends(get_current_user)
):
    """
    Get the player's saved territory color and structured flag configuration.
    """
    color = current_user.territory_color or "cyan"
    color_hex = STANDARD_TERRITORY_COLORS.get(color.lower(), color if color.startswith("#") else f"#{color}")
    is_custom = color.lower() not in STANDARD_TERRITORY_COLORS

    flag_raw = current_user.flag_config or {
        "background": "navy",
        "pattern": "diagonal",
        "emblem": "wolf",
        "border": "gold"
    }

    try:
        flag_obj = FlagConfig(**flag_raw)
    except Exception:
        flag_obj = FlagConfig(background="navy", pattern="diagonal", emblem="wolf", border="gold")

    return APIResponse(
        success=True,
        data=CustomizationResponse(
            territory_color=color,
            territory_color_hex=color_hex,
            is_custom_color=is_custom,
            flag=flag_obj,
            map_visibility_status="OPTIMAL_VISIBILITY"
        ),
        message="Customization retrieved successfully"
    )


@router.put("/users/me/customization", response_model=APIResponse[CustomizationResponse])
async def update_user_customization(
    body: CustomizationUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Update player visual customization (territory color + structured flag).
    Strictly validates colors, patterns, emblems, borders, and map visibility.
    Never trusts arbitrary client configuration.
    """
    color = body.territory_color.strip()
    is_custom = color.lower() not in STANDARD_TERRITORY_COLORS

    if is_custom:
        hex_code = color if color.startswith("#") else f"#{color}"
        valid, msg = validate_map_contrast(hex_code)
        if not valid:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail={"success": False, "error": "Map visibility check failed", "message": msg}
            )
        color_hex = hex_code.upper()
        saved_color = color_hex
    else:
        color_hex = STANDARD_TERRITORY_COLORS[color.lower()]
        saved_color = color.lower()

    # Save to user record in database
    current_user.territory_color = saved_color
    current_user.flag_config = body.flag.model_dump()

    db.add(current_user)
    await db.commit()
    await db.refresh(current_user)

    return APIResponse(
        success=True,
        data=CustomizationResponse(
            territory_color=saved_color,
            territory_color_hex=color_hex,
            is_custom_color=is_custom,
            flag=body.flag,
            map_visibility_status="OPTIMAL_VISIBILITY"
        ),
        message="Visual customization saved successfully"
    )
