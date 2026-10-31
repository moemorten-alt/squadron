from pydantic_settings import BaseSettings
from pydantic import field_validator
from typing import List
import json


class Settings(BaseSettings):
    database_url: str = "sqlite:///./squadron.db"
    jwt_secret: str = "dev-secret"
    jwt_expiration_seconds: int = 2592000  # 30 days
    cors_origins: List[str] = ["http://localhost:3000"]

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors(cls, v):
        if isinstance(v, str):
            return json.loads(v)
        return v

    model_config = {"env_file": ".env.local", "extra": "ignore"}


settings = Settings()
