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

    print("[2/3] Extracting & atomically switching /opt/chuxi/dist")
    # 与 deploy_upload.py 同口径：先解压到临时目录并校验顶层目录，再整体原子切换；
    # 任一环节失败即中止，线上现役 dist 绝不被提前删除
    cmds = [
        "rm -rf /tmp/dist-extract",
        "mkdir -p /tmp/dist-extract",
        "tar xzf /tmp/dist.tgz -C /tmp/dist-extract",
        "test -d /tmp/dist-extract/dist || exit 1",
        "ts=$(date +%Y%m%d%H%M%S)",
        "mv /opt/chuxi/dist /opt/chuxi/dist.old.$ts",
        # 第二个 mv 失败时自动回滚旧版本并中止，绝不让线上缺目录
        "mv /tmp/dist-extract/dist /opt/chuxi/dist || { mv /opt/chuxi/dist.old.$ts /opt/chuxi/dist; exit 1; }",
        "echo SWITCHED_TS=$ts",
        # 新包无 assets/ 时不应误判失败：只做统计展示，失败不影响切换结果判定
        "ls /opt/chuxi/dist/assets 2>/dev/null | wc -l || true",
    ]
    full = " && ".join(cmds)
    stdin, stdout, stderr = client.exec_command(full, timeout=120)
    out = stdout.read().decode(errors="replace").strip()
    err = stderr.read().decode(errors="replace").strip()
    if err:
        print(f"      [stderr] {err}")
    print(f"      {out}")
    if "SWITCHED_TS=" not in out:
        print("      [ERROR] dist switch failed; previous dist preserved under /opt/chuxi/dist.old.*", file=sys.stderr)
        sys.exit(1)

    print("[3/3] Health check (home page should return 200)")
    stdin2, stdout2, stderr2 = client.exec_command(
        "curl -sL -o /dev/null -w '%{http_code}' http://127.0.0.1/ 2>/dev/null || echo FAIL",
        timeout=30)
    code = stdout2.read().decode(errors="replace").strip()
    print(f"      HTTP {code}")
    if code != "200":
        print("      [ERROR] site not returning 200; rollback: mv /opt/chuxi/dist.old.<ts> /opt/chuxi/dist", file=sys.stderr)
        sys.exit(1)
    print("      nginx serves /opt/chuxi/dist directly; reload page to pick up new assets.")
finally:
    client.close()
