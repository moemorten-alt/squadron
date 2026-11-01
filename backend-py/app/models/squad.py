from sqlalchemy import Column, Integer, String, Text, DateTime  # noqa: F401
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class Squad(Base):
    __tablename__ = "squad"

    id          = Column(Integer, primary_key=True, autoincrement=True)
    name        = Column(String(100), nullable=False, unique=True)
    description = Column(Text)
    created_at  = Column(DateTime, server_default=func.now())
    updated_at  = Column(DateTime, server_default=func.now(), onupdate=func.now())

    allocations = relationship("Allocation", back_populates="squad", lazy="selectin",
                               cascade="all, delete-orphan")
