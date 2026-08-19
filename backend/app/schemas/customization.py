import re
from typing import Dict, Any, Optional, List
from pydantic import BaseModel, Field, field_validator

# Allowed territory color identifiers
STANDARD_TERRITORY_COLORS = {
    "blue": "#007AFF",
    "purple": "#9D00FF",
    "red": "#FF3B30",
    "orange": "#FF9500",
    "cyan": "#00F0FF",
    "pink": "#FF2D55",
    "gold": "#FFD700",
    "green": "#00E676",
    "indigo": "#4B0082",
}

# Allowed structured flag attributes
ALLOWED_FLAG_BACKGROUNDS = {
    "navy", "crimson", "emerald", "gold", "royal_blue", 
    "obsidian", "amethyst", "charcoal", "cyber_black"
}

ALLOWED_FLAG_PATTERNS = {
    "solid", "diagonal", "stripes_vertical", "stripes_horizontal", 
    "cross", "chevron", "split_diagonal", "checker"
}

ALLOWED_FLAG_EMBLEMS = {
    "wolf", "eagle", "falcon", "skull", "shield", "bolt", 
    "blade", "star", "crown", "dragon", "radar", "circuit"
}

ALLOWED_FLAG_BORDERS = {
    "none", "gold", "silver", "neon_cyan", "crimson", "double_gold", "carbon"
}


def validate_map_contrast(hex_color: str) -> tuple[bool, str]:
    """
    Validate visibility of a hex color against the tactical dark map background (#0E121A).
    Ensures adequate brightness and distinct color distance so sectors are clearly visible.
    """
    clean_hex = hex_color.strip().lstrip("#")
    if len(clean_hex) == 3:
        clean_hex = "".join([c * 2 for c in clean_hex])
    
    if len(clean_hex) != 6 or not re.match(r"^[0-9a-fA-F]{6}$", clean_hex):
        return False, "Invalid hex color format. Expected format #RRGGBB"
    
    r = int(clean_hex[0:2], 16)
    g = int(clean_hex[2:4], 16)
    b = int(clean_hex[4:6], 16)
    
    # Perceived brightness according to ITU-R BT.601
    brightness = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    
    # Check minimum brightness for visibility on dark map
    if brightness < 0.22:
        return False, f"Color brightness ({brightness:.2f}) is below minimum map visibility threshold (0.22). Select a brighter shade."
    
    # Check distance from dark map slate background #0E121A (RGB: 14, 18, 26)
    map_r, map_g, map_b = 14, 18, 26
    dist = ((r - map_r)**2 + (g - map_g)**2 + (b - map_b)**2)**0.5
    if dist < 45.0:
        return False, "Color blends into tactical map terrain. Select a higher contrast color."
    
    return True, "Valid map contrast"


class FlagConfig(BaseModel):
    background: str = Field(default="navy", description="Flag background shade")
    pattern: str = Field(default="diagonal", description="Flag geometric pattern")
    emblem: str = Field(default="wolf", description="Flag tactical emblem")
    border: str = Field(default="gold", description="Flag border style")

    @field_validator("background")
    @classmethod
    def validate_background(cls, v: str) -> str:
        val = v.lower().strip()
        if val not in ALLOWED_FLAG_BACKGROUNDS:
            raise ValueError(f"Invalid background '{val}'. Allowed: {', '.join(sorted(ALLOWED_FLAG_BACKGROUNDS))}")
        return val

    @field_validator("pattern")
    @classmethod
    def validate_pattern(cls, v: str) -> str:
        val = v.lower().strip()
        if val not in ALLOWED_FLAG_PATTERNS:
            raise ValueError(f"Invalid pattern '{val}'. Allowed: {', '.join(sorted(ALLOWED_FLAG_PATTERNS))}")
        return val

    @field_validator("emblem")
    @classmethod
    def validate_emblem(cls, v: str) -> str:
        val = v.lower().strip()
        if val not in ALLOWED_FLAG_EMBLEMS:
            raise ValueError(f"Invalid emblem '{val}'. Allowed: {', '.join(sorted(ALLOWED_FLAG_EMBLEMS))}")
        return val

    @field_validator("border")
    @classmethod
    def validate_border(cls, v: str) -> str:
        val = v.lower().strip()
        if val not in ALLOWED_FLAG_BORDERS:
            raise ValueError(f"Invalid border '{val}'. Allowed: {', '.join(sorted(ALLOWED_FLAG_BORDERS))}")
        return val


class CustomizationUpdateRequest(BaseModel):
    territory_color: str = Field(
        ..., 
        description="Named color (e.g. 'cyan', 'gold') or custom hex string ('#00FF88')"
    )
    flag: FlagConfig = Field(..., description="Structured flag composition")

    @field_validator("territory_color")
    @classmethod
    def validate_color(cls, v: str) -> str:
        val = v.lower().strip()
        if val in STANDARD_TERRITORY_COLORS:
            return val
        
        # Validate custom hex color
        if val.startswith("#") or len(val) == 6:
            hex_val = val if val.startswith("#") else f"#{val}"
            valid, msg = validate_map_contrast(hex_val)
            if not valid:
                raise ValueError(msg)
            return hex_val.upper()
        
        raise ValueError(
            f"Invalid territory color '{val}'. Must be one of: {', '.join(sorted(STANDARD_TERRITORY_COLORS.keys()))} "
            f"or a valid high-contrast hex color (e.g. #00FF88)"
        )


class CustomizationResponse(BaseModel):
    territory_color: str
    territory_color_hex: str
    is_custom_color: bool
    flag: FlagConfig
    map_visibility_status: str = "OPTIMAL_VISIBILITY"


class CustomizationOptionsResponse(BaseModel):
    standard_colors: Dict[str, str]
    backgrounds: List[str]
    patterns: List[str]
    emblems: List[str]
    borders: List[str]
