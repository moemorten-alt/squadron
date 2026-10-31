from pydantic import BaseModel
from typing import Optional, List


class CreatePersonRequest(BaseModel):
    name:       str
    email:      Optional[str] = None
    adminNote:  Optional[str] = None
    tagIds:     Optional[List[int]] = None
