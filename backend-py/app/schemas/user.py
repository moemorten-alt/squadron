from pydantic import BaseModel, field_validator
from typing import Optional


class CreateUserRequest(BaseModel):
    email:    str
    password: str
    role:     str

    @field_validator("role")
    @classmethod
    def check_role(cls, v: str) -> str:
        if v not in ("ADMIN", "VIEWER"):
            raise ValueError("role must be ADMIN or VIEWER")
        return v


class UpdateUserRequest(BaseModel):
    password: Optional[str] = None
    role:     Optional[str] = None
    active:   Optional[bool] = None

    @field_validator("role")
    @classmethod
    def check_role(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and v not in ("ADMIN", "VIEWER"):
            raise ValueError("role must be ADMIN or VIEWER")
        return v
