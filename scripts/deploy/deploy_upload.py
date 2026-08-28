#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
deploy_upload.py
================
全量部署：上传后端 jar + 前端 dist 包，并重启 chuxi.service。

先决条件：
- 设置 SSH_HOST、SSH_USER、SSH_KEY_PATH；SSH_USER 必须是最小权限部署账户，SSH_KEY_PATH 指向私钥
- 设置 SSH_KNOWN_HOSTS（默认 `~/.ssh/known_hosts`），其中必须已登记经人工核验的生产主机密钥
- 可选 JAR_LOCAL / DIST_TGZ 覆盖本地产物路径；可选 BACKUP_KEEP_DAYS 覆盖备份保留天数（默认 14）
- 后端 jar：在纯英文路径下 mvn package 产出（见 README「线上部署」构建路径坑）
- 前端 dist 包：cd frontend && npm run build，然后打包（tar 顶层必须是 dist/）：
    cd frontend && tar czf <DIST_TGZ> dist

安全/回滚设计（fail-closed + 自动回退）：
- 备份、上传、解压、健康检查任何一步失败都会中止部署，线上产物不被替换；
- 备份阶段在同一时间戳下保存 jar 与 dist 到 /opt/chuxi/backup/，失败即中止；
- 前端采用"解压到临时目录 -> 原子切换"流程，绝不先删线上 dist；
- 发布后执行健康检查套件：systemctl is-active、/actuator/health、本机外部首页、
  关键匿名 API（/api/front/home/landing → 200）、受保护 API（/api/admin/* → 401）；
- 任一检查失败自动回退到本次备份的 jar + dist 并复查；回退复查仍失败则退出码 2，提示人工介入；
- 部署成功后清理本次切换的 dist.old.* 与超过 BACKUP_KEEP_DAYS 天的历史备份。
"""

import os
import sys
import time
from datetime import datetime

import paramiko

HOST = os.environ.get("SSH_HOST", "")
PORT = int(os.environ.get("SSH_PORT", "22"))
USER = os.environ.get("SSH_USER", "")
KEY_PATH = os.path.expanduser(os.environ.get("SSH_KEY_PATH", ""))
KNOWN_HOSTS = os.path.expanduser(
    os.environ.get("SSH_KNOWN_HOSTS", "~/.ssh/known_hosts")
)
BACKUP_KEEP_DAYS = int(os.environ.get("BACKUP_KEEP_DAYS", "14"))

missing = [name for name, value in {
    "SSH_HOST": HOST,
    "SSH_USER": USER,
    "SSH_KEY_PATH": KEY_PATH,
}.items() if not value]
if missing:
    print(f"Set required environment variables: {', '.join(missing)}", file=sys.stderr)
    sys.exit(1)
if not os.path.isfile(KEY_PATH):
    print(f"SSH private key does not exist: {KEY_PATH}", file=sys.stderr)
    sys.exit(1)
if not os.path.isfile(KNOWN_HOSTS):
    print(f"SSH known_hosts does not exist: {KNOWN_HOSTS}", file=sys.stderr)
    sys.exit(1)

JAR_LOCAL = os.environ.get("JAR_LOCAL", r"D:/build/chuxi-backend2/target/chuxi-backend.jar")
DIST_TGZ = os.environ.get("DIST_TGZ", r"D:/workspace/dist_build.tgz")

if not os.path.isfile(JAR_LOCAL):
    print(f"[ABORT] 本地 jar 不存在：{JAR_LOCAL}", file=sys.stderr)
    sys.exit(1)
if not os.path.isfile(DIST_TGZ):
    print(f"[ABORT] 本地 dist 包不存在：{DIST_TGZ}", file=sys.stderr)
    sys.exit(1)

ssh = paramiko.SSHClient()
ssh.load_host_keys(KNOWN_HOSTS)
ssh.set_missing_host_key_policy(paramiko.RejectPolicy())
ssh.connect(
    HOST,
    port=PORT,
    username=USER,
    key_filename=KEY_PATH,
    look_for_keys=False,
    allow_agent=False,
    timeout=20,
)
s = ssh.open_sftp()


def run(cmd, timeout=60):
    """执行远程命令，返回 (stdout, exit_status)。"""
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    out = o.read().decode(errors="replace")
    err = e.read().decode(errors="replace")
    status = o.channel.recv_exit_status()
    print("CMD> ", cmd)
    if out.strip():
        print(out)
    if err.strip():
        print("[ERR]", err)
    return out, status


def run_or_abort(cmd, timeout=60):
    """备份/解压/健康检查等前置步骤失败时必须中止，避免在无回滚副本的情况下覆盖线上产物。"""
    out, status = run(cmd, timeout=timeout)
    if status != 0:
        print(f"[ABORT] 命令失败（exit={status}），中止部署，线上产物未被替换：{cmd}", file=sys.stderr)
        s.close()
        ssh.close()
        sys.exit(1)
    return out


def http_code(url, timeout=15):
    """返回 (http_code_str, exit_status)。"""
    return run(
        "curl -s -o /dev/null -w '%{http_code}' --max-time 10 '%s'" % url,
        timeout=timeout,
    )


def health_checks():
    """发布后健康检查套件。全部通过返回 True，任一失败打印失败项并返回 False。"""
    ok = True

    out, status = run("systemctl is-active chuxi")
    if status != 0 or "active" not in out:
        print("[HEALTH-FAIL] systemctl is-active chuxi", file=sys.stderr)
        ok = False

    code, _ = http_code("http://127.0.0.1:8080/actuator/health")
    if code.strip() != "200":
        print(f"[HEALTH-FAIL] /actuator/health -> {code.strip()}（期望 200）", file=sys.stderr)
        ok = False

    # 本机外部首页（nginx → 前端 dist），验证静态产物可服务
    code, _ = http_code("http://127.0.0.1/")
    if code.strip() != "200":
        print(f"[HEALTH-FAIL] 本机首页 / -> {code.strip()}（期望 200）", file=sys.stderr)
        ok = False

    # 关键匿名 API：首页聚合数据，验证后端业务链路可用
    code, _ = http_code("http://127.0.0.1:8080/api/front/home/landing")
    if code.strip() != "200":
        print(f"[HEALTH-FAIL] /api/front/home/landing -> {code.strip()}（期望 200）", file=sys.stderr)
        ok = False

    # 受保护接口：未登录必须 401，验证鉴权拦截器未被意外放宽
    code, _ = http_code("http://127.0.0.1:8080/api/admin/site-content")
    if code.strip() != "401":
        print(f"[HEALTH-FAIL] /api/admin/site-content -> {code.strip()}（期望 401）", file=sys.stderr)
        ok = False

    return ok


def rollback(ts):
    """自动回退到本次部署前备份的 jar + dist，重启并复查。复查通过返回 True。"""
    print(f"=== 自动回退到备份 {ts} ===", file=sys.stderr)

    # jar：备份存在才恢复（首次部署可能无备份）
    run(
        "if [ -f /opt/chuxi/backup/chuxi-backend.jar.bak.%s ]; then"
        " cp -f /opt/chuxi/backup/chuxi-backend.jar.bak.%s /opt/chuxi/chuxi-backend.jar && echo jar_restored;"
        " else echo 'no jar backup, skip'; fi" % (ts, ts)
    )

    # dist：先在同文件系统重建副本，再原子切换回去，避免 rm+cp 的中间态
    run(
        "if [ -d /opt/chuxi/backup/dist.bak.%s ]; then"
        " rm -rf /opt/chuxi/.dist.rollback.%s"
        " && cp -a /opt/chuxi/backup/dist.bak.%s /opt/chuxi/.dist.rollback.%s"
        " && { test ! -d /opt/chuxi/dist || mv /opt/chuxi/dist /opt/chuxi/dist.failed.%s; }"
        " && mv /opt/chuxi/.dist.rollback.%s /opt/chuxi/dist && echo dist_restored;"
        " else echo 'no dist backup, skip'; fi" % (ts, ts, ts, ts, ts, ts)
    )

    run("systemctl restart chuxi")
    print("... 等待 Spring Boot 启动（回退后复查） ...")
    time.sleep(25)
    return health_checks()


ts = datetime.now().strftime("%Y%m%d%H%M%S")

# 1. 备份现行 jar + dist（带时间戳，供回滚；必须在任何替换动作之前完成，失败即中止）
#    回滚步骤见 README「线上部署 · 回滚 runbook」。
print("=== 1. 备份现行 jar + dist ===")
run_or_abort(
    "mkdir -p /opt/chuxi/backup"
    " && { test ! -f /opt/chuxi/chuxi-backend.jar || cp -f /opt/chuxi/chuxi-backend.jar /opt/chuxi/backup/chuxi-backend.jar.bak.%s; }"
    " && { test ! -d /opt/chuxi/dist || cp -a /opt/chuxi/dist /opt/chuxi/backup/dist.bak.%s; }"
    " && ls -d /opt/chuxi/backup/*.%s >/dev/null 2>&1 && echo backed_up" % (ts, ts, ts)
)

# 2. 上传新 jar
print("=== 2. 上传 jar ===")
s.put(JAR_LOCAL, "/opt/chuxi/chuxi-backend.jar")
print("jar uploaded -> /opt/chuxi/chuxi-backend.jar")

# 3. 上传 dist 包
print("=== 3. 上传 dist.tgz ===")
s.put(DIST_TGZ, "/tmp/dist.tgz")
print("dist.tgz uploaded -> /tmp/dist.tgz")

# 4. 解压到临时目录并原子切换 dist（不先删线上目录；任一失败线上 dist 仍在）
#    tar 内顶层必须是 dist/（本地打包：cd <dist父目录> && tar czf <DIST_TGZ> dist）
#    dist.old.<ts> 保留到健康检查全部通过后再清理，作为切换期的第二份回退副本。
print("=== 4. 解压 dist（原子切换）===")
run_or_abort(
    "rm -rf /tmp/dist-extract && mkdir -p /tmp/dist-extract"
    " && tar xzf /tmp/dist.tgz -C /tmp/dist-extract"
    " && test -d /tmp/dist-extract/dist && echo dist_extracted"
)
run_or_abort(
    "if [ -d /opt/chuxi/dist ]; then mv /opt/chuxi/dist /opt/chuxi/dist.old.%s; fi"
    " && mv /tmp/dist-extract/dist /opt/chuxi/dist"
    " && ls /opt/chuxi/dist | head" % ts
)

# 5. 重启 chuxi
print("=== 5. 重启 chuxi.service ===")
run("systemctl restart chuxi")
print("... 等待 Spring Boot 启动 ...")
time.sleep(25)

# 6. 健康检查套件；任一失败自动回退并复查
print("=== 6. 健康检查（is-active / actuator / 首页 / 匿名 API / 受保护 401）===")
if not health_checks():
    print("[WARN] 健康检查未全部通过，启动自动回退 ...", file=sys.stderr)
    run("journalctl -u chuxi -n 40 --no-pager", timeout=30)
    if rollback(ts):
        print("[ROLLBACK-OK] 已自动回退到上一版本且复查通过。本次发布未生效，请排查后重发。",
              file=sys.stderr)
        s.close()
        ssh.close()
        sys.exit(1)
    print("[ROLLBACK-FAIL] 自动回退后复查仍未通过，立即人工介入（见 README 回滚 runbook）！",
          file=sys.stderr)
    run("journalctl -u chuxi -n 40 --no-pager", timeout=30)
    s.close()
    ssh.close()
    sys.exit(2)
print("health checks passed")

# 7. 部署成功：清理切换期 dist.old 与超过保留期的历史备份
print("=== 7. 清理切换残留与过期备份（保留 %d 天）===" % BACKUP_KEEP_DAYS)
run("rm -rf /opt/chuxi/dist.old.%s /opt/chuxi/dist.failed.%s" % (ts, ts))
run(
    "find /opt/chuxi/backup -maxdepth 1 \\( -name '*.bak.*' -o -name 'dist.bak.*' \\)"
    " -mtime +%d -print -delete" % BACKUP_KEEP_DAYS
)

print("=== 启动日志尾部 ===")
run("journalctl -u chuxi -n 40 --no-pager", timeout=30)

s.close()
ssh.close()
print("DONE")
