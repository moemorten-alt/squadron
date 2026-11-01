# Import all models so Base.metadata is fully populated (needed by Alembic)
from app.models.lookup import Technology, DeveloperRole, Tag  # noqa: F401
from app.models.user import AppUser, UserRole                 # noqa: F401
from app.models.person import Person, person_tag              # noqa: F401
from app.models.squad import Squad                            # noqa: F401
from app.models.allocation import (                           # noqa: F401
    Allocation, allocation_role, allocation_technology,
)
