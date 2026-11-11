#!/usr/bin/env bash
# End-to-end proof for the wired Neon services.
# Requires the API running on API (default http://localhost:8080) and .env.local.
set -uo pipefail

API="${API:-http://localhost:8080}"
BASE=$(grep '^NEON_AUTH_BASE_URL=' .env.local | cut -d= -f2-)
ADMIN_EMAIL=$(grep '^ADMIN_EMAIL=' .env.local | cut -d= -f2-)
ADMIN_PASSWORD="${ADMIN_PASSWORD:?set ADMIN_PASSWORD to the local admin password}"
JAR=$(mktemp)
EMAIL="e2e-safina-$(date +%s)@example.com"

jget() { node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{const o=JSON.parse(d);const v=process.argv[1].split('.').reduce((a,k)=>a==null?a:a[k],o);console.log(v??'')}catch(e){console.log('')}})" "$1"; }

step() { printf '\n=== %s ===\n' "$1"; }

step "1. Neon Auth: sign up a test user and mint a JWT"
curl -s -c "$JAR" -X POST "$BASE/sign-up/email" \
  -H 'content-type: application/json' -H "Origin: $API" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Safina-E2E-pass-123!\",\"name\":\"E2E\"}" \
  | jget user.id | sed 's/^/neon auth user id: /'
TOKEN=$(curl -s -b "$JAR" "$BASE/token" -H "Origin: $API" | jget token)
echo "jwt: ${TOKEN:0:24}... (${#TOKEN} chars)"

step "2. Health (Postgres reachable)"
curl -s -w '\nHTTP %{http_code}\n' "$API/health"

step "3. Protected request: GET /auth/me verifies the JWT via NEON_AUTH_JWKS_URL"
curl -s -w '\nHTTP %{http_code}\n' "$API/auth/me" -H "Authorization: Bearer $TOKEN"

step "4. Invalid token is rejected"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' "$API/auth/me" -H "Authorization: Bearer not-a-real-jwt"

step "5. Channel name: availability + claim"
CHANNEL="e2e_$(date +%s | tail -c 7)"
curl -s -w '\n' "$API/profile/channel/available?name=$CHANNEL" -H "Authorization: Bearer $TOKEN"
curl -s -w '\n' -X PUT "$API/profile/channel" -H "Authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' -d "{\"channelName\":\"$CHANNEL\"}"

step "6. Publish a public scene (Neon Postgres + scene storage)"
SCENE_ID=$(curl -s -X POST "$API/public/scenes" -H "Authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' \
  -d '{"title":"E2E Scene","html":"<!doctype html><html><body style=\"background:#123;color:#fff\"><h1>E2E</h1></body></html>"}' \
  | tee /tmp/e2e_publish.json | jget scene.id)
cat /tmp/e2e_publish.json; echo
echo "scene id: $SCENE_ID"

step "7. Read it back (proves the storage backend round-trip)"
curl -s "$API/public/scenes/$SCENE_ID" -H "Authorization: Bearer $TOKEN" | head -c 400; echo

step "8. Explore list (pagination envelope)"
curl -s "$API/public/scenes?page=1&limit=5&sort=new" -H "Authorization: Bearer $TOKEN" | head -c 300; echo

step "9. Save the scene and check saved_count moved in the same transaction"
curl -s -w '\n' -X POST "$API/scenes/$SCENE_ID/save" -H "Authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' -d '{"collectionId":null}'

step "10. Collections: create one, move the save into it, list"
COL_ID=$(curl -s -X POST "$API/collections" -H "Authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' -d '{"name":"Evening"}' | jget collection.id)
echo "collection id: $COL_ID"
curl -s -w '\n' -X POST "$API/scenes/$SCENE_ID/save" -H "Authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' -d "{\"collectionId\":\"$COL_ID\"}"
curl -s -w '\n' "$API/collections" -H "Authorization: Bearer $TOKEN"

step "11. GET /me/saves"
curl -s "$API/me/saves" -H "Authorization: Bearer $TOKEN" | head -c 300; echo

step "12. Report a scene"
curl -s -w '\n' -X POST "$API/scenes/$SCENE_ID/report" -H "Authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' -d '{"reason":"E2E check"}'

step "13. Admin: non-admin token gets 404 on admin routes"
curl -s -w '\nHTTP %{http_code}\n' "$API/admin/reports" -H "Authorization: Bearer $TOKEN"

step "14. Admin login"
LOGIN=$(curl -s -X POST "$API/admin/login" -H 'content-type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")
echo "$LOGIN" | head -c 160; echo
ADMIN=$(echo "$LOGIN" | jget token)

step "15. Admin adds a default scene, then /default-scenes serves it with an ETag"
DEFAULT_ID=$(curl -s -X POST "$API/admin/default-scenes" -H "Authorization: Bearer $ADMIN" \
  -H 'content-type: application/json' \
  -d '{"title":"Admin Default","html":"<!doctype html><html><body>admin default</body></html>","sortOrder":9}' \
  | jget scene.id)
echo "default scene id: $DEFAULT_ID"
ETAG=$(curl -s -D - -o /tmp/e2e_defaults.json "$API/default-scenes" | grep -i '^etag:' | tr -d '\r' | cut -d' ' -f2)
echo "etag: $ETAG"; head -c 220 /tmp/e2e_defaults.json; echo
curl -s -o /dev/null -w 'conditional GET with If-None-Match -> HTTP %{http_code}\n' \
  "$API/default-scenes" -H "If-None-Match: $ETAG"

step "16. Admin moderation list + dismiss"
curl -s "$API/admin/reports" -H "Authorization: Bearer $ADMIN" | head -c 300; echo
curl -s -w '\n' -X POST "$API/admin/reports/$SCENE_ID/dismiss" -H "Authorization: Bearer $ADMIN"

step "17. Admin removes the public scene, author sees it as removed in Saved"
curl -s -w '\n' -X DELETE "$API/admin/public-scenes/$SCENE_ID" -H "Authorization: Bearer $ADMIN" \
  -H 'content-type: application/json' -d '{"reason":"e2e moderation"}'
curl -s "$API/me/saves" -H "Authorization: Bearer $TOKEN" | head -c 260; echo

step "18. Admin audit log"
curl -s "$API/admin/audit-log" -H "Authorization: Bearer $ADMIN" | head -c 260; echo

rm -f "$JAR"
