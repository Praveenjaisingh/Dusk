
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
else
  echo "Error: .env file not found. Please create one based on .env.example"
  exit 1
fi

# Ensure variables are set
: "${DB_URL:?DB_URL needs to be set in .env}"
: "${DB_USERNAME:?DB_USERNAME needs to be set in .env}"
: "${DB_PASSWORD:?DB_PASSWORD needs to be set in .env}"

DB_USER="$DB_USERNAME"
DB_PASS="$DB_PASSWORD"


DB_HOST=$(echo "$DB_URL" | sed -E 's#jdbc:postgresql://([^:/]+).*#\1#')
DB_PORT=$(echo "$DB_URL" | sed -E 's#jdbc:postgresql://[^:]+:([0-9]+)/.*#\1#')
DB_NAME=$(echo "$DB_URL" | sed -E 's#.*/([^/?]+).*#\1#')

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}

echo "Database : $DB_NAME"
echo "Host     : $DB_HOST"
echo "Port     : $DB_PORT"
echo "User     : $DB_USER"
echo

echo "This will TRUNCATE all tables in '$DB_NAME'. All data will be lost."
read -p "Continue? [y/N] " CONFIRM

if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    echo "Cancelled."
    exit 0
fi

echo "Truncating all tables in '$DB_NAME'..."

PGPASSWORD="$DB_PASS" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -c "DO \$\$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
    LOOP
        EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' CASCADE';
    END LOOP;
END
\$\$;"

echo "All tables truncated successfully."
echo

echo "Starting Spring Boot (seeder will re-populate data)..."

mvn spring-boot:run
