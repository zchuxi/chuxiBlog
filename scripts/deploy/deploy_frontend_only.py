#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
deploy_frontend_only.py
=======================
仅部署前端：上传 dist 包并在服务器端解压覆盖，跳过后端重启。前端-only 改动后使用。

先决条件：
- 设置 SSH_HOST、SSH_USER、SSH_KEY_PATH；SSH_USER 必须是最小权限部署账户
- 设置 SSH_KNOWN_HOSTS（默认 `~/.ssh/known_hosts`），其中必须已登记经人工核验的生产主机密钥
- 可选 DIST_TGZ 覆盖本地产物路径
- 前端 dist 包：cd frontend && npm run build，然后打包（tar 顶层必须是 dist/）：
    cd frontend && tar czf <DIST_TGZ> dist

安全/回滚设计：
- 前端采用"解压到临时目录 -> 原子切换"流程，绝不先删线上 dist；
- 切换后执行健康检查（本机首页 200 + 关键匿名 API 200），失败时自动回退到切换前版本并复查；
- 自动回退复查仍失败退出码 2，提示人工介入。
"""

import os
import sys
import paramiko

HOST = os.environ.get("SSH_HOST", "")
PORT = int(os.environ.get("SSH_PORT", "22"))
USER = os.environ.get("SSH_USER", "")
KEY_PATH = os.path.expanduser(os.environ.get("SSH_KEY_PATH", ""))
KNOWN_HOSTS = os.path.expanduser(
    os.environ.get("SSH_KNOWN_HOSTS", "~/.ssh/known_hosts")
)
DIST_TGZ = os.environ.get("DIST_TGZ", r"D:\workspace\dist_build.tgz")

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
if not os.path.isfile(DIST_TGZ):
    print(f"dist package does not exist: {DIST_TGZ}", file=sys.stderr)
    sys.exit(1)

print(f"[1/3] Uploading {DIST_TGZ} -> /tmp/dist.tgz")
client = paramiko.SSHClient()
client.load_host_keys(KNOWN_HOSTS)
client.set_missing_host_key_policy(paramiko.RejectPolicy())
client.connect(
    HOST,
    port=PORT,
    username=USER,
    key_filename=KEY_PATH,
    look_for_keys=False,
    allow_agent=False,
    timeout=20,
)
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

    print("[3/4] Health check (home page 200 + anonymous API 200)")
    healthy = True
    for label, url in (("home page", "http://127.0.0.1/"),
                       ("anonymous API", "http://127.0.0.1:8080/api/front/home/landing")):
        stdin2, stdout2, stderr2 = client.exec_command(
            "curl -s -o /dev/null -w '%{http_code}' --max-time 10 '%s' 2>/dev/null || echo FAIL" % url,
            timeout=30)
        code = stdout2.read().decode(errors="replace").strip()
        print(f"      {label}: HTTP {code}")
        if code != "200":
            healthy = False
    if not healthy:
        # 自动回退：从输出中取切换时间戳，把 dist.old.<ts> 原子换回，再复查首页
        switched_ts = ""
        for line in out.splitlines():
            if line.startswith("SWITCHED_TS="):
                switched_ts = line.split("=", 1)[1].strip()
        if switched_ts:
            print("      [WARN] health check failed, rolling back automatically ...", file=sys.stderr)
            rb = ("mv /opt/chuxi/dist /opt/chuxi/dist.failed.%s"
                  " && mv /opt/chuxi/dist.old.%s /opt/chuxi/dist"
                  " && curl -s -o /dev/null -w '%%{http_code}' --max-time 10 http://127.0.0.1/"
                  % (switched_ts, switched_ts))
            stdin3, stdout3, stderr3 = client.exec_command(rb, timeout=60)
            rb_code = stdout3.read().decode(errors="replace").strip()
            print(f"      rollback done, home page now HTTP {rb_code}")
            if rb_code == "200":
                print("      [ROLLBACK-OK] reverted to previous dist; this release did not take effect.", file=sys.stderr)
                sys.exit(1)
            print("      [ROLLBACK-FAIL] still unhealthy after rollback; manual intervention required!", file=sys.stderr)
            sys.exit(2)
        print("      [ERROR] site unhealthy and no SWITCHED_TS found; rollback manually: mv /opt/chuxi/dist.old.<ts> /opt/chuxi/dist", file=sys.stderr)
        sys.exit(1)

    print("[4/4] Cleanup switched-out dist.old.*")
    switched_ts = ""
    for line in out.splitlines():
        if line.startswith("SWITCHED_TS="):
            switched_ts = line.split("=", 1)[1].strip()
    if switched_ts:
        client.exec_command("rm -rf /opt/chuxi/dist.old.%s /opt/chuxi/dist.failed.%s" % (switched_ts, switched_ts), timeout=30)
    print("      nginx serves /opt/chuxi/dist directly; reload page to pick up new assets.")
finally:
    client.close()
