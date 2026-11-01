from pydantic import BaseModel, field_validator
from typing import Optional, List


class CreateAllocationRequest(BaseModel):
    personId:          int
    squadId:           int
    roleIds:           List[int]
    technologyIds:     Optional[List[int]] = None
    allocationPercent: int
    publicComment:     Optional[str] = None
    adminNote:         Optional[str] = None
    startDate:         Optional[str] = None
    endDate:           Optional[str] = None

    @field_validator("allocationPercent")
    @classmethod
    def check_percent(cls, v: int) -> int:
        if not 0 <= v <= 100:
            raise ValueError("allocationPercent must be between 0 and 100")
        return v
