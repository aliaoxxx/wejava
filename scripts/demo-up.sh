#!/usr/bin/env bash
#
# WeJavaAdmin 一键演示启动
#
#   ./scripts/demo-up.sh            # 启动并等待健康检查
#   WEB_PORT=3301 ./scripts/demo-up.sh
#   TIMEOUT=1200 ./scripts/demo-up.sh   # 网络慢时放宽等待时间
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

WEB_PORT="${WEB_PORT:-3300}"
TIMEOUT="${TIMEOUT:-900}"   # 首次构建要拉镜像 + 编译，默认等 15 分钟

c_info() { printf '\033[36m%s\033[0m\n' "$*"; }
c_ok()   { printf '\033[32m%s\033[0m\n' "$*"; }
c_warn() { printf '\033[33m%s\033[0m\n' "$*"; }
c_err()  { printf '\033[31m%s\033[0m\n' "$*" >&2; }

require_docker() {
    # Docker Desktop 有时只把 CLI 放在 ~/.docker/bin，没有加进 PATH
    if ! command -v docker >/dev/null 2>&1 && [ -x "$HOME/.docker/bin/docker" ]; then
        export PATH="$HOME/.docker/bin:$PATH"
    fi

    command -v docker >/dev/null 2>&1 || {
        c_err "未找到 docker 命令。请先安装 Docker Desktop 或 Docker Engine："
        c_err "  https://docs.docker.com/get-docker/"
        c_err "  macOS 若已安装仍提示找不到，在 Docker Desktop 设置里勾选"
        c_err "  Advanced → Install CLI tools，或把 ~/.docker/bin 加入 PATH" 
        exit 1
    }
    if ! docker info >/dev/null 2>&1; then
        c_err "Docker 守护进程没有运行。"
        c_err "  macOS：打开 Docker Desktop，等鲸鱼图标稳定后重试"
        c_err "  Linux：sudo systemctl start docker"
        exit 1
    fi
    docker compose version >/dev/null 2>&1 || {
        c_err "未找到 docker compose（v2）。请升级 Docker 或安装 compose 插件。"
        exit 1
    }
}

prepare_env() {
    if [ ! -f .env ] && [ -f .env.example ]; then
        cp .env.example .env
        c_warn "未发现 .env，已从 .env.example 生成（端口 ${WEB_PORT}、默认密码）"
    fi
}

wait_healthy() {
    local start now elapsed total healthy
    start=$(date +%s)
    c_info "等待服务健康检查（首次构建请耐心，最长 ${TIMEOUT} 秒）..."

    while :; do
        total=$(docker compose ps --services 2>/dev/null | wc -l | tr -d ' ')
        healthy=$(docker compose ps --format '{{.Service}} {{.Health}}' 2>/dev/null \
                  | grep -c ' healthy$' || true)

        now=$(date +%s)
        elapsed=$((now - start))

        if [ "$total" -ge 4 ] && [ "$healthy" -ge 4 ]; then
            c_ok "全部服务就绪（用时 ${elapsed} 秒）"
            return 0
        fi

        if [ "$elapsed" -ge "$TIMEOUT" ]; then
            c_err "等待超时（${TIMEOUT} 秒），当前状态："
            docker compose ps
            c_err ""
            c_err "排查建议："
            c_err "  docker compose logs --tail=80 backend"
            c_err "  docker compose logs --tail=40 mysql"
            return 1
        fi

        printf '\r  已运行 %3d 秒，健康服务 %d/%d ...' "$elapsed" "$healthy" "$total"
        sleep 5
    done
}

print_result() {
    local port
    port=$(grep -E '^WEB_PORT=' .env 2>/dev/null | cut -d= -f2 | tr -d ' ' || true)
    port="${port:-$WEB_PORT}"

    echo
    c_ok "=============================================="
    c_ok "  WeJavaAdmin 已启动"
    c_ok "=============================================="
    echo "  访问地址   http://localhost:${port}/"
    echo "  账号       admin / admin123"
    echo
    echo "  查看日志   docker compose logs -f backend"
    echo "  停止服务   docker compose down"
    echo "  彻底重置   docker compose down -v && ./scripts/demo-up.sh"
    echo
    c_warn "首次登录后建议在「个人中心」修改默认密码。"
    echo
}

main() {
    require_docker
    prepare_env

    c_info "启动容器（首次会构建镜像，可能需要 5–10 分钟）..."
    docker compose up -d

    wait_healthy
    print_result
}

main "$@"
