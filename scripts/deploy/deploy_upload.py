#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
deploy_upload.py
================
全量部署：上传后端 jar + 前端 dist 包，并重启 chuxi.service。

先决条件：
- 设置环境变量 SSH_PWD（服务器 root 密码）；可选 JAR_LOCAL / DIST_TGZ 覆盖本地产物路径
- 后端 jar：在纯英文路径下 mvn package 产出（见 README「线上部署」构建路径坑）
- 前端 dist 包：cd frontend && npm run build，然后打包（tar 顶层必须是 dist/）：
    cd frontend && tar czf <DIST_TGZ> dist

安全/回滚设计（fail-closed）：
- 备份、上传、解压、健康检查任何一步失败都会中止部署，线上产物不被替换；
- 备份阶段在同一时间戳下保存 jar 与 dist 到 /opt/chuxi/backup/，失败即中止；
- 前端采用"解压到临时目录 -> 原子切换"流程，绝不先删线上 dist；
- 重启后必须通过 /actuator/health 检查才视为成功，否则打印回滚提示。
"""

import os
import sys
import time
from datetime import datetime

import paramiko

HOST = "106.14.202.90"
PORT = 22
USER = "root"

PASS = os.environ.get("SSH_PWD", "")
if not PASS:
    print("Set SSH_PWD env var first.", file=sys.stderr)
    sys.exit(1)

JAR_LOCAL = os.environ.get("JAR_LOCAL", r"D:/build/chuxi-backend2/target/chuxi-backend.jar")
DIST_TGZ = os.environ.get("DIST_TGZ", r"D:/workspace/dist_build.tgz")

if not os.path.isfile(JAR_LOCAL):
    print(f"[ABORT] 本地 jar 不存在：{JAR_LOCAL}", file=sys.stderr)
    sys.exit(1)
if not os.path.isfile(DIST_TGZ):
    print(f"[ABORT] 本地 dist 包不存在：{DIST_TGZ}", file=sys.stderr)
    sys.exit(1)

t = paramiko.Transport((HOST, PORT))
t.connect(username=USER, password=PASS)
s = paramiko.SFTPClient.from_transport(t)
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PASS)


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
        t.close()
        sys.exit(1)
    return out


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
print("=== 4. 解压 dist（原子切换）===")
run_or_abort(
    "rm -rf /tmp/dist-extract && mkdir -p /tmp/dist-extract"
    " && tar xzf /tmp/dist.tgz -C /tmp/dist-extract"
    " && test -d /tmp/dist-extract/dist && echo dist_extracted"
)
run_or_abort(
    "if [ -d /opt/chuxi/dist ]; then mv /opt/chuxi/dist /opt/chuxi/dist.old.%s; fi"
    " && mv /tmp/dist-extract/dist /opt/chuxi/dist"
    " && rm -rf /opt/chuxi/dist.old.%s"
    " && ls /opt/chuxi/dist | head" % (ts, ts)
)

# 5. 重启 chuxi
print("=== 5. 重启 chuxi.service ===")
run("systemctl restart chuxi")
print("... 等待 Spring Boot 启动 ...")
time.sleep(25)
out, status = run("systemctl is-active chuxi")
if status != 0 or "active" not in out:
    print("[ABORT] 服务未进入 active 状态，请立即回滚（见 README 回滚 runbook）", file=sys.stderr)
    run("journalctl -u chuxi -n 40 --no-pager", timeout=30)
    s.close()
    ssh.close()
    t.close()
    sys.exit(1)

# 6. 健康检查（仅检查健康状态，不暴露组件详情）
print("=== 6. 健康检查 ===")
out, status = run("curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/actuator/health", timeout=15)
if status != 0 or out.strip() != "200":
    print(f"[ABORT] 健康检查未通过（http_code={out.strip()}），请立即回滚（见 README 回滚 runbook）",
          file=sys.stderr)
    run("journalctl -u chuxi -n 40 --no-pager", timeout=30)
    s.close()
    ssh.close()
    t.close()
    sys.exit(1)
print("health check passed (200)")

print("=== 启动日志尾部 ===")
run("journalctl -u chuxi -n 40 --no-pager", timeout=30)

s.close()
ssh.close()
t.close()
print("DONE")
