"""Add territory_color and flag_config to users

Revision ID: 003_add_customization_and_flag
Revises: 002_create_users_and_sessions
Create Date: 2026-08-18 00:02:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = '003_add_customization_and_flag'
down_revision: Union[str, None] = '002_create_users_and_sessions'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        'users',
        sa.Column('territory_color', sa.String(length=30), nullable=False, server_default='cyan')
    )
    op.add_column(
        'users',
        sa.Column('flag_config', sa.JSON(), nullable=True)
    )


def downgrade() -> None:
    op.drop_column('users', 'flag_config')
    op.drop_column('users', 'territory_color')
