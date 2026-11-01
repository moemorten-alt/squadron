"""Init schema

Revision ID: 001
Revises:
Create Date: 2025-01-01 00:00:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "app_user",
        sa.Column("id",            sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("email",         sa.String(255),  nullable=False,   unique=True),
        sa.Column("password_hash", sa.String(255),  nullable=False),
        sa.Column("user_role",     sa.String(20),   nullable=False,   server_default="VIEWER"),
        sa.Column("active",        sa.Boolean(),    nullable=False,   server_default=sa.text("TRUE")),
        sa.Column("created_at",    sa.DateTime(),   nullable=False,   server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("updated_at",    sa.DateTime(),   nullable=False,   server_default=sa.text("CURRENT_TIMESTAMP")),
    )

    op.create_table(
        "squad",
        sa.Column("id",          sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("name",        sa.String(100),  nullable=False,   unique=True),
        sa.Column("description", sa.Text()),
        sa.Column("created_at",  sa.DateTime(),   nullable=False,   server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("updated_at",  sa.DateTime(),   nullable=False,   server_default=sa.text("CURRENT_TIMESTAMP")),
    )

    op.create_table(
        "person",
        sa.Column("id",         sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("name",       sa.String(255),  nullable=False),
        sa.Column("email",      sa.String(255)),
        sa.Column("active",     sa.Boolean(),    nullable=False, server_default=sa.text("TRUE")),
        sa.Column("admin_note", sa.Text()),
        sa.Column("created_at", sa.DateTime(),   nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("updated_at", sa.DateTime(),   nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
    )

    op.create_table(
        "technology",
        sa.Column("id",   sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("name", sa.String(100),  nullable=False,   unique=True),
    )

    op.create_table(
        "developer_role",
        sa.Column("id",   sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("name", sa.String(100),  nullable=False,   unique=True),
    )

    op.create_table(
        "tag",
        sa.Column("id",   sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("name", sa.String(100),  nullable=False,   unique=True),
        sa.Column("slug", sa.String(100),  nullable=False,   unique=True),
    )

    op.create_table(
        "person_tag",
        sa.Column("person_id", sa.BigInteger(), sa.ForeignKey("person.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("tag_id",    sa.BigInteger(), sa.ForeignKey("tag.id",    ondelete="CASCADE"), primary_key=True),
    )

    op.create_table(
        "allocation",
        sa.Column("id",                 sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("person_id",          sa.BigInteger(), sa.ForeignKey("person.id", ondelete="CASCADE"), nullable=False),
        sa.Column("squad_id",           sa.BigInteger(), sa.ForeignKey("squad.id",  ondelete="CASCADE"), nullable=False),
        sa.Column("allocation_percent", sa.Integer(),    nullable=False),
        sa.Column("public_comment",     sa.Text()),
        sa.Column("admin_note",         sa.Text()),
        sa.Column("start_date",         sa.Date()),
        sa.Column("end_date",           sa.Date()),
        sa.Column("active",             sa.Boolean(),    nullable=False, server_default=sa.text("TRUE")),
        sa.Column("created_at",         sa.DateTime(),   nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
        sa.Column("updated_at",         sa.DateTime(),   nullable=False, server_default=sa.text("CURRENT_TIMESTAMP")),
    )

    op.create_table(
        "allocation_role",
        sa.Column("allocation_id",     sa.BigInteger(), sa.ForeignKey("allocation.id",     ondelete="CASCADE"), primary_key=True),
        sa.Column("developer_role_id", sa.BigInteger(), sa.ForeignKey("developer_role.id", ondelete="CASCADE"), primary_key=True),
    )

    op.create_table(
        "allocation_technology",
        sa.Column("allocation_id", sa.BigInteger(), sa.ForeignKey("allocation.id",  ondelete="CASCADE"), primary_key=True),
        sa.Column("technology_id", sa.BigInteger(), sa.ForeignKey("technology.id",  ondelete="CASCADE"), primary_key=True),
    )

    op.create_index("idx_allocation_person", "allocation", ["person_id"])
    op.create_index("idx_allocation_squad",  "allocation", ["squad_id"])
    op.create_index("idx_allocation_active", "allocation", ["active"])
    op.create_index("idx_person_active",     "person",     ["active"])


def downgrade() -> None:
    op.drop_table("allocation_technology")
    op.drop_table("allocation_role")
    op.drop_table("allocation")
    op.drop_table("person_tag")
    op.drop_table("tag")
    op.drop_table("developer_role")
    op.drop_table("technology")
    op.drop_table("person")
    op.drop_table("squad")
    op.drop_table("app_user")
