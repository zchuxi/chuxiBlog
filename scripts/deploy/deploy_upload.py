#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
deploy_upload.py
================
全量部署：上传后端 jar + 前端 dist 包，并重启 chuxi.service。

先决条件：
- 设置环境变量 SSH_PWD（服务器 root 密码），可选 JAR_LOCAL / DIST_TGZ 覆盖本地产物路径
- 后端 jar：在纯英文路径下 mvn package 产出（见 README「线上部署」构建路径坑）
- 前端 dist 包：cd frontend && npm run build，然后打包（tar 顶层必须是 dist/）：
    cd frontend && tar czf <DIST_TGZ> dist
"""
import os, paramiko, sys, time
from datetime import datetime

HOST = "106.14.202.90"
PORT = 22
USER = "root"
PASS = os.environ.get("SSH_PWD", "")
if not PASS:
    print("Set SSH_PWD env var first.", file=sys.stderr)
    sys.exit(1)

JAR_LOCAL = os.environ.get("JAR_LOCAL", r"D:/build/chuxi2-backend/target/chuxi-backend.jar")
DIST_TGZ = os.environ.get("DIST_TGZ", r"D:/workspace/dist_build.tgz")

t = paramiko.Transport((HOST, PORT))
t.connect(username=USER, password=PASS)
s = paramiko.SFTPClient.from_transport(t)
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PASS)

def run(cmd, timeout=60):
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
    # 备份等前置步骤失败时必须中止部署，避免在无回滚副本的情况下覆盖线上产物
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
run_or_abort(f"mkdir -p /opt/chuxi/backup"
             f" && {{ test ! -f /opt/chuxi/chuxi-backend.jar || cp -f /opt/chuxi/chuxi-backend.jar /opt/chuxi/backup/chuxi-backend.jar.bak.{ts}; }}"
             f" && {{ test ! -d /opt/chuxi/dist || cp -a /opt/chuxi/dist /opt/chuxi/backup/dist.bak.{ts}; }}"
             f" && {{ ls -d /opt/chuxi/backup/*.{ts} 2>/dev/null || true; }} && echo backed_up")

# 2. 上传新 jar
print("=== 2. 上传 jar ===")
s.put(JAR_LOCAL, "/opt/chuxi/chuxi-backend.jar")
print("jar uploaded -> /opt/chuxi/chuxi-backend.jar")

# 3. 上传 dist 包
print("=== 3. 上传 dist.tgz ===")
s.put(DIST_TGZ, "/tmp/dist.tgz")
print("dist.tgz uploaded -> /tmp/dist.tgz")

# 4. 服务器端解压覆盖 dist
#    tar 内顶层必须是 dist/，否则会解到 /opt/chuxi/ 散落文件（首次部署时踩过此坑）。
#    本地打包命令：cd frontend && tar czf <DIST_TGZ> dist
print("=== 4. 解压 dist ===")
run("rm -rf /opt/chuxi/dist && mkdir -p /opt/chuxi/dist && tar xzf /tmp/dist.tgz -C /opt/chuxi/ && echo dist_extracted && ls /opt/chuxi/dist | head")

# 5. 重启 chuxi
print("=== 5. 重启 chuxi.service ===")
run("systemctl restart chuxi")
print("... 等待 Spring Boot 启动 ...")
time.sleep(25)
run("systemctl is-active chuxi")
print("=== 启动日志尾部 ===")
run("journalctl -u chuxi -n 40 --no-pager", timeout=30)

s.close()
ssh.close()
t.close()
print("DONE")
