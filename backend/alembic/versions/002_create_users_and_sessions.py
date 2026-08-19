"""Create users and user_sessions tables

Revision ID: 002_create_users_and_sessions
Revises: 001_initial_postgis
Create Date: 2026-08-18 00:01:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = '002_create_users_and_sessions'
down_revision: Union[str, None] = '001_initial_postgis'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create enum type for faction
    op.execute("DO $$ BEGIN CREATE TYPE factionenum AS ENUM ('APEX', 'CIPHER', 'SOLARIS'); EXCEPTION WHEN duplicate_object THEN null; END $$;")

    # Create users table
    op.create_table(
        'users',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('email', sa.String(length=255), nullable=False),
        sa.Column('hashed_password', sa.String(length=255), nullable=True),
        sa.Column('display_name', sa.String(length=100), nullable=False),
        sa.Column('avatar_url', sa.String(length=512), nullable=True),
        sa.Column('faction', sa.Enum('APEX', 'CIPHER', 'SOLARIS', name='factionenum'), nullable=False, server_default='CIPHER'),
        sa.Column('google_id', sa.String(length=255), nullable=True),
        sa.Column('auth_provider', sa.String(length=50), nullable=False, server_default='password'),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('is_verified', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('total_area_sq_meters', sa.Float(), nullable=False, server_default='0.0'),
        sa.Column('total_distance_meters', sa.Float(), nullable=False, server_default='0.0'),
        sa.Column('territories_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_users_id'), 'users', ['id'], unique=False)
    op.create_index(op.f('ix_users_email'), 'users', ['email'], unique=True)
    op.create_index(op.f('ix_users_google_id'), 'users', ['google_id'], unique=True)

    # Create user_sessions table
    op.create_table(
        'user_sessions',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('user_id', sa.String(length=36), nullable=False),
        sa.Column('refresh_token', sa.String(length=512), nullable=False),
        sa.Column('user_agent', sa.String(length=255), nullable=True),
        sa.Column('ip_address', sa.String(length=64), nullable=True),
        sa.Column('is_revoked', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('expires_at', sa.DateTime(), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('last_used_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_user_sessions_id'), 'user_sessions', ['id'], unique=False)
    op.create_index(op.f('ix_user_sessions_user_id'), 'user_sessions', ['user_id'], unique=False)
    op.create_index(op.f('ix_user_sessions_refresh_token'), 'user_sessions', ['refresh_token'], unique=True)


def downgrade() -> None:
    op.drop_table('user_sessions')
    op.drop_table('users')
    op.execute("DROP TYPE IF EXISTS factionenum;")
