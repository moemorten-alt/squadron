from sqlalchemy import Column, Integer, Boolean, Text, Date, DateTime, Table, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


allocation_role = Table(
    "allocation_role",
    Base.metadata,
    Column("allocation_id",     Integer, ForeignKey("allocation.id",     ondelete="CASCADE"), primary_key=True),
    Column("developer_role_id", Integer, ForeignKey("developer_role.id", ondelete="CASCADE"), primary_key=True),
)

allocation_technology = Table(
    "allocation_technology",
    Base.metadata,
    Column("allocation_id", Integer, ForeignKey("allocation.id",  ondelete="CASCADE"), primary_key=True),
    Column("technology_id", Integer, ForeignKey("technology.id",  ondelete="CASCADE"), primary_key=True),
)


class Allocation(Base):
    __tablename__ = "allocation"

    id                 = Column(Integer, primary_key=True, autoincrement=True)
    person_id          = Column(Integer, ForeignKey("person.id", ondelete="CASCADE"), nullable=False)
    squad_id           = Column(Integer, ForeignKey("squad.id",  ondelete="CASCADE"), nullable=False)
    allocation_percent = Column(Integer, nullable=False)
    public_comment     = Column(Text)
    admin_note         = Column(Text)
    start_date         = Column(Date)
    end_date           = Column(Date)
    active             = Column(Boolean, nullable=False, default=True)
    created_at         = Column(DateTime, server_default=func.now())
    updated_at         = Column(DateTime, server_default=func.now(), onupdate=func.now())

    person       = relationship("Person",       back_populates="allocations", lazy="joined")
    squad        = relationship("Squad",        back_populates="allocations", lazy="joined")
    roles        = relationship("DeveloperRole", secondary=allocation_role,       lazy="selectin")
    technologies = relationship("Technology",   secondary=allocation_technology,  lazy="selectin")
