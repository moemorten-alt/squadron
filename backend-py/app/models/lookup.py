from sqlalchemy import Column, Integer, String
from app.database import Base


class Technology(Base):
    __tablename__ = "technology"

    id   = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(100), nullable=False, unique=True)


class DeveloperRole(Base):
    __tablename__ = "developer_role"

    id   = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(100), nullable=False, unique=True)


class Tag(Base):
    __tablename__ = "tag"

    id   = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(100), nullable=False, unique=True)
    slug = Column(String(100), nullable=False, unique=True)
