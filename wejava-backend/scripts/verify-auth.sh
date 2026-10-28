#!/usr/bin/env bash
# ============================================================
# M2 认证闭环验收脚本
#
# 前提：
#   1. 后端已启动，且关闭了验证码（脚本无法识别图片）：
#        CAPTCHA_ENABLED=false mvn spring-boot:run
#   2. 数据库里有 admin / admin123
#   3. 如需测试 403，数据库里需有 tester / admin123（角色 common，无任何权限）
#
# 用法：
#   ./scripts/verify-auth.sh                      # 默认 http://127.0.0.1:8000/dev/api
#   ./scripts/verify-auth.sh http://host:8000/api
# ============================================================
set -uo pipefail

BASE="${1:-http://127.0.0.1:8000/dev/api}"
PASS=0
FAIL=0

jqv() { python3 -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }

# check <描述> <期望的 code> <json 文本>
check() {
  local name="$1" expect="$2" body="$3"
  local actual
  actual=$(echo "$body" | jqv "d.get('code')")
  if [ "$actual" = "$expect" ]; then
    printf '  ✅ %-38s code=%s\n' "$name" "$actual"
    PASS=$((PASS + 1))
  else
    printf '  ❌ %-38s 期望 %s 实际 %s | %s\n' "$name" "$expect" "${actual:-<非JSON>}" "$(echo "$body" | head -c 120)"
    FAIL=$((FAIL + 1))
  fi
}

# check_status <描述> <期望 HTTP> <实际 HTTP>
check_status() {
  local name="$1" expect="$2" actual="$3"
  if [ "$actual" = "$expect" ]; then
    printf '  ✅ %-38s HTTP %s\n' "$name" "$actual"
    PASS=$((PASS + 1))
  else
    printf '  ❌ %-38s 期望 HTTP %s 实际 %s\n' "$name" "$expect" "$actual"
    FAIL=$((FAIL + 1))
  fi
}

echo "=============================================="
echo " 认证闭环验收  BASE=$BASE"
echo "=============================================="

echo
echo "[1] 基础连通"
check "GET /ping" 200 "$(curl -s "$BASE/ping")"

echo
echo "[2] 登录"
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
check "admin 登录成功" 200 "$LOGIN"
ACCESS=$(echo "$LOGIN" | jqv "d['data']['accessToken']")
REFRESH=$(echo "$LOGIN" | jqv "d['data']['refreshToken']")

if [ -z "$ACCESS" ] || [ "$ACCESS" = "None" ]; then
  echo "  ⚠️  拿不到 token，后续用例跳过"
  echo
  echo "结果：通过 $PASS，失败 $FAIL"
  exit 1
fi

echo
echo "[3] 鉴权"
check "带 token 访问 /auth/me" 200 "$(curl -s "$BASE/auth/me" -H "Authorization: Bearer $ACCESS")"
check "不带 token 访问 /auth/me" 1006 "$(curl -s "$BASE/auth/me")"

FORGED=$(python3 -c "
s='$ACCESS'
i=len(s)-3
c='A' if s[i]!='A' else 'B'
print(s[:i]+c+s[i+1:])")
check "篡改签名" 1006 "$(curl -s "$BASE/auth/me" -H "Authorization: Bearer $FORGED")"
check "refresh 当 access 用" 1006 "$(curl -s "$BASE/auth/me" -H "Authorization: Bearer $REFRESH")"

echo
echo "[4] 刷新与轮换"
R2=$(curl -s -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}")
check "refresh 成功" 200 "$R2"
NEW_ACCESS=$(echo "$R2" | jqv "d['data']['accessToken']")
check "新 access 可用" 200 "$(curl -s "$BASE/auth/me" -H "Authorization: Bearer $NEW_ACCESS")"
check "旧 refresh 重复使用被拒" 1006 "$(curl -s -X POST "$BASE/auth/refresh" \
  -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}")"

echo
echo "[5] 权限"
check "管理员访问受保护接口" 200 "$(curl -s "$BASE/ping/secure" -H "Authorization: Bearer $NEW_ACCESS")"

TOKEN_T=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"tester","password":"admin123"}' | jqv "d['data']['accessToken']")
if [ -n "$TOKEN_T" ] && [ "$TOKEN_T" != "None" ]; then
  status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/ping/secure" -H "Authorization: Bearer $TOKEN_T")
  check_status "无权限用户访问受保护接口" 403 "$status"
else
  printf '  ⏭  跳过 403 用例（数据库里没有 tester 账号）\n'
fi

echo
echo "[6] 登出与撤销"
check "登出" 200 "$(curl -s -X POST "$BASE/auth/logout" -H "Authorization: Bearer $NEW_ACCESS")"
check "登出后旧 token 失效" 1006 "$(curl -s "$BASE/auth/me" -H "Authorization: Bearer $NEW_ACCESS")"

echo
echo "[7] 防爆破（用不存在的账号，避免锁死 admin）"
for i in 1 2 3 4 5; do
  curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d '{"username":"nosuchuser","password":"wrong"}' >/dev/null
done
check "第 6 次提示账号锁定" 1003 "$(curl -s -X POST "$BASE/auth/login" \
  -H 'Content-Type: application/json' -d '{"username":"nosuchuser","password":"wrong"}')"

echo
echo "=============================================="
printf " 结果：通过 %d，失败 %d\n" "$PASS" "$FAIL"
echo "=============================================="
echo "提示：如需清理测试残留，执行"
echo "  REDISCLI_AUTH=<密码> redis-cli --scan --pattern 'admin:auth:*' | xargs redis-cli del"

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
