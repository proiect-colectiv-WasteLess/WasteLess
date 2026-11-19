"""Reject edits/deletes of baseline migrations and feature-owned SQL."""
import re
import subprocess
import sys
from pathlib import Path
root = Path(__file__).resolve().parents[2]
files = list((root / 'backend/src/main/resources').rglob('*.sql'))
versions = []
for path in files:
    relative = path.relative_to(root).as_posix()
    if relative == 'backend/src/main/resources/db/dev/R__demo_data.sql':
        continue
    if not relative.startswith('backend/src/main/resources/db/migration/'):
        raise SystemExit(f'SQL outside the platform migration stream: {relative}')
    match = re.fullmatch(r'V([0-9]+)__[a-z0-9_]+\.sql', path.name)
    if not match:
        raise SystemExit(f'Invalid migration name: {path.name}')
    versions.append(int(match[1]))
if len(versions) != len(set(versions)):
    raise SystemExit('Duplicate Flyway version')
base = sys.argv[1] if len(sys.argv) > 1 else ''
if base and set(base) != {'0'}:
    changes = subprocess.check_output(['git', 'diff', '--name-status', '--no-renames', base, 'HEAD', '--', 'backend/src/main/resources/db/migration'], cwd=root, text=True)
    for line in changes.splitlines():
        if not line.startswith('A\t'):
            raise SystemExit(f'Existing migrations are immutable: {line}')
print('Migration governance passed')
