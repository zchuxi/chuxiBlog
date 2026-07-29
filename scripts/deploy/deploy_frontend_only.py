#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
deploy_frontend_only.py
=======================
仅部署前端：上传 dist 包并在服务器端解压覆盖，跳过后端重启。前端-only 改动后使用。

先决条件：
- 设置环境变量 SSH_PWD（服务器 root 密码），可选 DIST_TGZ 覆盖本地产物路径
- 前端 dist 包：cd frontend && npm run build，然后打包（tar 顶层必须是 dist/）：
    cd frontend && tar czf <DIST_TGZ> dist
"""

import os
import sys
import paramiko

HOST = "106.14.202.90"
USER = "root"
PASSWORD = os.environ.get("SSH_PWD", "")
DIST_TGZ = os.environ.get("DIST_TGZ", r"D:\workspace\dist_build.tgz")

if not PASSWORD:
    print("Set SSH_PWD env var first.", file=sys.stderr)
    sys.exit(1)

print(f"[1/3] Uploading {DIST_TGZ} -> /tmp/dist.tgz")
client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, port=22, username=USER, password=PASSWORD, timeout=20)
try:
    sftp = client.open_sftp()
    sftp.put(DIST_TGZ, "/tmp/dist.tgz")
    sftp.close()
    print("      done")

    print("[2/3] Extracting to /opt/chuxi/dist on server")
    cmds = [
        "rm -rf /opt/chuxi/dist",
        "mkdir -p /opt/chuxi/dist",
        "tar xzf /tmp/dist.tgz -C /opt/chuxi/",
        "echo OK",
        "ls /opt/chuxi/dist/assets | wc -l",
    ]
    full = " && ".join(cmds)
    stdin, stdout, stderr = client.exec_command(full, timeout=60)
    out = stdout.read().decode(errors="replace").strip()
    err = stderr.read().decode(errors="replace").strip()
    if err:
        print(f"      [stderr] {err}")
    print(f"      {out}")

    print("[3/3] No backend restart needed (frontend-only change)")
    print("      nginx serves /opt/chuxi/dist directly; reload page to pick up new assets.")
finally:
    client.close()
