from sqlalchemy import Column, Integer, String, Boolean, Text, DateTime, Table, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


person_tag = Table(
    "person_tag",
    Base.metadata,
    Column("person_id", Integer, ForeignKey("person.id", ondelete="CASCADE"), primary_key=True),
    Column("tag_id",    Integer, ForeignKey("tag.id",    ondelete="CASCADE"), primary_key=True),
)


class Person(Base):
    __tablename__ = "person"

    id         = Column(Integer, primary_key=True, autoincrement=True)
    name       = Column(String(255), nullable=False)
    email      = Column(String(255))
    active     = Column(Boolean, nullable=False, default=True)
    admin_note = Column(Text)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    tags        = relationship("Tag",        secondary=person_tag,  lazy="selectin")
    allocations = relationship("Allocation", back_populates="person", lazy="selectin",
                               cascade="all, delete-orphan")
