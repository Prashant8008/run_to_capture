from pydantic_settings import BaseSettings
from typing import Optional
import urllib.parse

class Settings(BaseSettings):
    PROJECT_NAME: str = "Run2Capture Backend"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Database (PostgreSQL + PostGIS)
    DATABASE_URI_OVERRIDE: Optional[str] = None
    POSTGRES_SERVER: str = "db.txdwsmlscxvqyqomzwzq.supabase.co"
    POSTGRES_PORT: int = 5432
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "Prashant123!@#$%^&*()"
    POSTGRES_DB: str = "postgres"
    
    # Supabase Client Credentials
    SUPABASE_URL: str = "https://txdwsmlscxvqyqomzwzq.supabase.co"
    SUPABASE_KEY: str = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InR4ZHdzbWxzY3h2cXlxb216d3pxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc5MjUyMDUsImV4cCI6MjEwMzUwMTIwNX0.URoMXFO6iujeMXeqhlhmTlQdTCXI2X0Loog8JxxJC-E"
    
    # Redis
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: Optional[str] = None
    
    # Authentication & Security
    JWT_SECRET_KEY: str = "run2capture-development-secret-key-change-in-production-super-secure"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 # 24 hours
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30
    GOOGLE_CLIENT_ID: Optional[str] = None
    
    @property
    def encoded_password(self) -> str:
        return urllib.parse.quote_plus(self.POSTGRES_PASSWORD)

    @property
    def DATABASE_URL(self) -> str:
        if self.DATABASE_URI_OVERRIDE:
            uri = self.DATABASE_URI_OVERRIDE
            if uri.startswith("postgresql://"):
                return uri.replace("postgresql://", "postgresql+asyncpg://", 1)
            return uri
        return f"postgresql+asyncpg://{self.POSTGRES_USER}:{self.encoded_password}@{self.POSTGRES_SERVER}:{self.POSTGRES_PORT}/{self.POSTGRES_DB}"
        
    @property
    def SYNC_DATABASE_URL(self) -> str:
        if self.DATABASE_URI_OVERRIDE:
            uri = self.DATABASE_URI_OVERRIDE
            if uri.startswith("postgresql+asyncpg://"):
                return uri.replace("postgresql+asyncpg://", "postgresql://", 1)
            return uri
        return f"postgresql://{self.POSTGRES_USER}:{self.encoded_password}@{self.POSTGRES_SERVER}:{self.POSTGRES_PORT}/{self.POSTGRES_DB}"
        
    @property
    def REDIS_URL(self) -> str:
        if self.REDIS_PASSWORD:
            return f"redis://:{self.REDIS_PASSWORD}@{self.REDIS_HOST}:{self.REDIS_PORT}/0"
        return f"redis://{self.REDIS_HOST}:{self.REDIS_PORT}/0"

    class Config:
        case_sensitive = True
        env_file = ".env"
        extra = "allow"

settings = Settings()
