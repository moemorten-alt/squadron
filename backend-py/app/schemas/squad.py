from pydantic import BaseModel
from typing import Optional


class CreateSquadRequest(BaseModel):
    name:        str
    description: Optional[str] = None
