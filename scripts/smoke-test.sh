#!/usr/bin/env bash
# End-to-end smoke test for a running Qaryati stack.
#
#   cp .env.example .env
#   docker compose up --build        (wait until the app says it started)
#   bash scripts/smoke-test.sh       (from another terminal, in the project folder; Git Bash works on Windows)
#
# It registers three users, promotes two of them in the database, and walks through the main flows:
# contribute, review, search, statistics, nearby, geocoding, update/delete, roles and the audit log.
# Every check prints PASS or FAIL. The exit code is the number of failures.
#
# It creates test data (users named smoke_*, villages described "Smoke test"). Run it against a local database only.
# It uses 3 register calls plus 1 login, under the login rate limit of 10 per minute. Wait a minute before running it twice.

set -u

BASE="${BASE_URL:-http://localhost:8080}"
API="$BASE/api/v1"
CONTAINER="${DB_CONTAINER:-qaryati-db}"

if [ -f .env ]; then set -a; . ./.env; set +a; fi
DB_USER="${DB_USERNAME:-qaryati_user}"
DB_NAME="${DB_NAME:-qaryati}"

TMP="$(mktemp)"

# Pre-flight: stop early with one clear message instead of dozens of failures
if ! curl -s -o /dev/null --max-time 5 "$BASE/v3/api-docs"; then
  echo "The app is not answering at $BASE."
  echo "Start it with 'docker compose up --build', wait until the log says the application has started,"
  echo "then run this script again from a second terminal. To see why it is down: docker compose ps"
  echo "and docker compose logs app --tail 40"
  rm -f "$TMP"
  exit 2
fi
if ! docker exec "$CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
  echo "The app answers, but the database container '$CONTAINER' is not reachable with 'docker exec'."
  echo "Run this script from the project folder (it reads .env there), and check the container name with: docker ps"
  rm -f "$TMP"
  exit 2
fi

PASSES=0
FAILS=0
STATUS=""
BODY=""

pass() { PASSES=$((PASSES + 1)); printf '  PASS  %s\n' "$1"; }
fail() { FAILS=$((FAILS + 1)); printf '  FAIL  %s\n' "$1"; }

# req METHOD PATH [TOKEN] [JSON]  -> sets STATUS and BODY
req() {
  local method="$1" path="$2" token="${3:-}" data="${4:-}"
  local args=(-s -o "$TMP" -w '%{http_code}' -X "$method" "$API$path" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-d "$data")
  STATUS="$(curl "${args[@]}")"
  BODY="$(cat "$TMP")"
}

# expect "description" "200" or "200|201"
expect() {
  local ok=0 o
  IFS='|' read -ra options <<< "$2"
  for o in "${options[@]}"; do [ "$STATUS" = "$o" ] && ok=1; done
  if [ "$ok" = 1 ]; then pass "$1 ($STATUS)"; else fail "$1 (expected $2, got $STATUS) $BODY"; fi
}

# has "description" "text that must appear in the last response body"
has() {
  case "$BODY" in
    *"$2"*) pass "$1" ;;
    *) fail "$1 (missing '$2') $BODY" ;;
  esac
}

json_id() { printf '%s' "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }
json_token() { printf '%s' "$BODY" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4; }

db() { docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1" 2>/dev/null; }

# register USERNAME -> sets STATUS, BODY and TOKEN_OUT (not run in a subshell, so STATUS survives)
register() {
  req POST /auth/register "" "{\"username\":\"$1\",\"email\":\"$1@example.com\",\"password\":\"password123\"}"
  TOKEN_OUT="$(json_token)"
}

SUFFIX="$(date +%s)"
CONTRIB_NAME="smoke_contrib_$SUFFIX"
VERIFIER_NAME="smoke_verifier_$SUFFIX"
ADMIN_NAME="smoke_admin_$SUFFIX"

echo "Qaryati smoke test against $BASE"
echo

echo "1. Users and roles"
register "$CONTRIB_NAME"
CONTRIB="$TOKEN_OUT"
expect "register a contributor" "200|201"
[ -n "$CONTRIB" ] && pass "register returns a token" || fail "register returns a token"
register "$VERIFIER_NAME"
VERIFIER="$TOKEN_OUT"
register "$ADMIN_NAME"
ADMIN="$TOKEN_OUT"
db "UPDATE users SET role='VERIFIER' WHERE username='$VERIFIER_NAME'" > /dev/null
db "UPDATE users SET role='ADMIN' WHERE username='$ADMIN_NAME'" > /dev/null
[ "$(db "SELECT role FROM users WHERE username='$ADMIN_NAME'")" = "ADMIN" ] \
  && pass "promoted the admin in the database (docker exec $CONTAINER)" \
  || fail "could not promote users: is the container '$CONTAINER' running and .env present?"
req POST /auth/login "" "{\"username\":\"$CONTRIB_NAME\",\"password\":\"wrong-password\"}"
expect "login with a wrong password is refused" "401"

echo
echo "2. Public reads and authentication"
req GET /governorates
expect "list governorates (public)" "200"
has "governorates include Nablus" "Nablus"
req GET /villages
expect "list villages (public)" "200"
req POST /villages "" '{"governorateId":1,"latitude":32.25,"longitude":35.3,"locationDescription":"Smoke test","elevationM":600}'
expect "create a village without a token is refused" "401"
req POST /villages "$CONTRIB" '{"governorateId":1,"latitude":999,"longitude":35.3,"locationDescription":"Smoke test","elevationM":600}'
expect "invalid latitude is rejected" "422"
has "validation error code" "VALIDATION_FAILED"

echo
echo "3. Contribute and review"
req POST /villages "$CONTRIB" '{"governorateId":1,"latitude":32.25,"longitude":35.3,"locationDescription":"Smoke test village","elevationM":600}'
expect "contributor creates a village" "201"
VID="$(json_id)"
[ -n "$VID" ] && pass "village id is $VID" || fail "no village id in the response"

req POST "/villages/$VID/population" "$CONTRIB" '{"population":5000,"year":2020,"source":"Smoke test census"}'
expect "contributor submits a population record" "201"
has "new record is PENDING" "PENDING"
PID="$(json_id)"
req POST "/villages/$VID/population" "$CONTRIB" '{"population":5100,"year":2020,"source":"Smoke test census"}'
expect "same village, year and source again is refused" "409"

req GET "/villages/$VID/population/stats?fromYear=1800&toYear=2100"
expect "statistics (public)" "200"
has "pending records do not count" '"recordCount":0'

req PATCH "/villages/$VID/population/$PID/review" "$CONTRIB" '{"status":"VERIFIED","reviewNotes":"self"}'
expect "a contributor cannot review" "403"
req PATCH "/villages/$VID/population/$PID/review" "$VERIFIER" '{"status":"VERIFIED","reviewNotes":"checked against the census"}'
expect "a verifier approves the record" "200"
req PATCH "/villages/$VID/population/$PID/review" "$VERIFIER" '{"status":"REJECTED","reviewNotes":"again"}'
expect "a reviewed record cannot be reviewed again" "422"
req GET "/villages/$VID/population/stats?fromYear=1800&toYear=2100"
has "verified record is counted" '"recordCount":1'
has "statistics show the population" '"minPopulation":5000'

echo
echo "4. Names, search and geocoding"
EN_NAME="SmokeTown$SUFFIX"
req POST "/villages/$VID/names" "$CONTRIB" "{\"name\":\"$EN_NAME\",\"nameType\":\"current\",\"language\":\"en\",\"source\":\"Smoke test\"}"
expect "contributor adds an English name" "201"
EN_ID="$(json_id)"
req PATCH "/villages/$VID/names/$EN_ID/review" "$VERIFIER" '{"status":"VERIFIED","reviewNotes":"ok"}'
expect "verifier approves the English name" "200"
req GET "/villages?q=$EN_NAME"
expect "search by name" "200"
has "search finds the village through its name" "\"id\":$VID"

req GET "/villages/$VID/geocode-check"
expect "geocode check needs a verified Arabic name (expect 422 before)" "422"
req POST "/villages/$VID/names" "$CONTRIB" '{"name":"\u062d\u0648\u0627\u0631\u0629","nameType":"current","language":"ar","source":"Smoke test"}'
expect "contributor adds an Arabic name" "201"
AR_ID="$(json_id)"
req PATCH "/villages/$VID/names/$AR_ID/review" "$VERIFIER" '{"status":"VERIFIED","reviewNotes":"ok"}'
expect "verifier approves the Arabic name" "200"
req GET "/villages/$VID/geocode-check"
expect "geocode check (200 needs internet access to Nominatim, 503 means it is unreachable)" "200|503"
echo "        geocode-check answered $STATUS"

req GET "/villages/$VID/nearby?radiusKm=50&limit=5"
expect "nearby villages" "200"

echo
echo "5. Update, delete and roles"
req PUT "/villages/$VID" "$CONTRIB" '{"governorateId":1,"latitude":32.26,"longitude":35.3,"locationDescription":"Updated by contributor","elevationM":610}'
expect "a contributor cannot update a village" "403"
req PUT "/villages/$VID" "$ADMIN" '{"governorateId":1,"latitude":32.26,"longitude":35.3,"locationDescription":"Smoke test village updated","elevationM":610}'
expect "an admin updates the village" "200"
req DELETE "/villages/$VID" "$ADMIN"
expect "a village with population records cannot be deleted" "409"
has "delete conflict code" "VILLAGE_DELETE_CONFLICT"

req POST /villages "$ADMIN" '{"governorateId":1,"latitude":32.3,"longitude":35.3,"locationDescription":"Smoke test throwaway","elevationM":500}'
THROWAWAY="$(json_id)"
req DELETE "/villages/$THROWAWAY" "$ADMIN"
expect "an admin deletes a village without records" "204"

CONTRIB_ID="$(db "SELECT id FROM users WHERE username='$CONTRIB_NAME'")"
req PATCH "/users/$CONTRIB_ID/role" "$CONTRIB" '{"role":"ADMIN"}'
expect "a contributor cannot change roles" "403"
req PATCH "/users/$CONTRIB_ID/role" "$ADMIN" '{"role":"VERIFIER"}'
expect "an admin sets a role" "200"
ADMIN_ID="$(db "SELECT id FROM users WHERE username='$ADMIN_NAME'")"
req PATCH "/users/$ADMIN_ID/role" "$ADMIN" '{"role":"CONTRIBUTOR"}'
expect "an admin cannot demote themselves" "409"

echo
echo "6. Audit log and documentation"
N="$(db "SELECT count(*) FROM audit_logs WHERE entity_type='VILLAGE' AND action IN ('CREATE','UPDATE','DELETE') AND entity_id IN ($VID,${THROWAWAY:-0})")"
[ "${N:-0}" -ge 4 ] && pass "village create, update and delete are in the audit log ($N rows)" || fail "expected 4+ village audit rows, found '${N:-none}'"
N="$(db "SELECT count(*) FROM audit_logs WHERE action='REVIEW' AND entity_type IN ('POPULATION_RECORD','VILLAGE_NAME') AND entity_id IN ($PID,$EN_ID,$AR_ID)")"
[ "${N:-0}" -ge 3 ] && pass "review decisions are in the audit log ($N rows)" || fail "expected 3 review audit rows, found '${N:-none}'"
N="$(db "SELECT count(*) FROM audit_logs WHERE action='ROLE_CHANGE' AND entity_id=$CONTRIB_ID")"
[ "${N:-0}" -ge 1 ] && pass "the role change is in the audit log" || fail "no ROLE_CHANGE audit row"

CODE="$(curl -s -o "$TMP" -w '%{http_code}' "$BASE/v3/api-docs")"
BODY="$(cat "$TMP")"
STATUS="$CODE"
expect "OpenAPI document is served" "200"
has "OpenAPI document declares the bearer scheme" "bearerAuth"
CODE="$(curl -s -o /dev/null -w '%{http_code}' "$BASE/swagger-ui.html")"
case "$CODE" in 200|302) pass "Swagger UI is served ($CODE)" ;; *) fail "Swagger UI returned $CODE" ;; esac

rm -f "$TMP"
echo
echo "Result: $PASSES passed, $FAILS failed"
exit "$FAILS"