import uuid
from datetime import datetime
from sqlalchemy import Column, String, Boolean, DateTime, Float, Integer, JSON, Enum as SQLEnum
from sqlalchemy.orm import relationship
import enum
from backend.app.database import Base

class FactionEnum(str, enum.Enum):
    APEX = "APEX"
    CIPHER = "CIPHER"
    SOLARIS = "SOLARIS"

class User(Base):
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()), index=True)
    email = Column(String(255), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=True) # Nullable for pure OAuth users
    display_name = Column(String(100), nullable=False)
    avatar_url = Column(String(512), nullable=True)
    faction = Column(SQLEnum(FactionEnum), default=FactionEnum.CIPHER, nullable=False)
    
    # Customization & Flag
    territory_color = Column(String(30), default="cyan", nullable=False)
    flag_config = Column(JSON, default=lambda: {
        "background": "navy",
        "pattern": "diagonal",
        "emblem": "wolf",
        "border": "gold"
    }, nullable=True)
    
    # OAuth Provider tracking
    google_id = Column(String(255), unique=True, index=True, nullable=True)
    auth_provider = Column(String(50), default="password", nullable=False) # "password" | "google"
    
    is_active = Column(Boolean, default=True, nullable=False)
    is_verified = Column(Boolean, default=False, nullable=False)
    
    # Aggregated Stats
    total_area_sq_meters = Column(Float, default=0.0, nullable=False)
    total_distance_meters = Column(Float, default=0.0, nullable=False)
    territories_count = Column(Integer, default=0, nullable=False)
    
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    # Relationships
    sessions = relationship("UserSession", back_populates="user", cascade="all, delete-orphan")
