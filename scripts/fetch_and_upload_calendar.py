#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
fetch_and_upload_calendar.py
============================
本地拉取 Bangumi.tv 每周放送日历，上传到服务器磁盘缓存。
服务器后端（BangumiCalendarService.fetchCalendar）会优先读这个缓存，
所以即使服务器无法访问 api.bgm.tv（GFW/IP 级拦截），日历页面也能正常显示。

使用方法：
  1. 打开你本机的代理（Clash / v2rayN / ...），确保 HTTPS_PROXY 已设
  2. 设置 SSH_HOST、SSH_USER、SSH_KEY_PATH（最小权限账户 + 私钥，禁止密码），
     可选 SSH_KNOWN_HOSTS（默认 ~/.ssh/known_hosts，须已登记经人工核验的主机密钥）
  3. python fetch_and_upload_calendar.py
  4. 想每天自动跑，加 Windows 任务计划

依赖：见 scripts/requirements.txt（requests, paramiko）
"""

import os
import sys
import json
import datetime

import requests
import paramiko

# ---------- 配置 ----------
HOST = os.environ.get("SSH_HOST", "")
PORT = int(os.environ.get("SSH_PORT", "22"))
USER = os.environ.get("SSH_USER", "")
KEY_PATH = os.path.expanduser(os.environ.get("SSH_KEY_PATH", ""))
KNOWN_HOSTS = os.path.expanduser(
    os.environ.get("SSH_KNOWN_HOSTS", "~/.ssh/known_hosts")
)

_missing = [n for n, v in {"SSH_HOST": HOST, "SSH_USER": USER, "SSH_KEY_PATH": KEY_PATH}.items() if not v]
if _missing:
    print(f"Set required environment variables: {', '.join(_missing)}", file=sys.stderr)
    sys.exit(1)
if not os.path.isfile(KEY_PATH):
    print(f"SSH private key does not exist: {KEY_PATH}", file=sys.stderr)
    sys.exit(1)
if not os.path.isfile(KNOWN_HOSTS):
    print(f"SSH known_hosts does not exist: {KNOWN_HOSTS}", file=sys.stderr)
    sys.exit(1)

API_URL = "https://api.bgm.tv/calendar"
# 与后端 BangumiCalendarService 的相对缓存路径 data/bangumi-calendar.json 对应
# （后端运行目录 /opt/chuxi -> 实际文件 /opt/chuxi/data/bangumi-calendar.json）
REMOTE_PATH = "/opt/chuxi/data/bangumi-calendar.json"
LOCAL_BACKUP = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "bangumi-calendar.local.json",
)

# 代理：依次尝试环境变量里的 HTTPS_PROXY / HTTP_PROXY / ALL_PROXY
PROXY = (
    os.environ.get("HTTPS_PROXY")
    or os.environ.get("https_proxy")
    or os.environ.get("HTTP_PROXY")
    or os.environ.get("http_proxy")
    or os.environ.get("ALL_PROXY")
)
PROXIES = {"http": PROXY, "https": PROXY} if PROXY else None
# ---------- /配置 ----------


def fetch_calendar():
    print(f"[1/3] Fetching {API_URL}")
    print(f"      proxy = {PROXY or '(direct)'}")
    resp = requests.get(
        API_URL,
        proxies=PROXIES,
        timeout=20,
        headers={
            "User-Agent": "chuxi-web-fetch/1.0 (https://www.chuxi.online)",
            "Accept": "application/json",
        },
    )
    resp.raise_for_status()
    data = resp.json()
    if not isinstance(data, list):
        raise ValueError(f"Unexpected payload: not a list (got {type(data).__name__})")
    total = sum(len(e.get("items", [])) for e in data)
    print(f"      OK -> {len(data)} days, {total} subjects, {len(resp.content)} bytes")
    return data


def save_local(data):
    print(f"[2/3] Saving local backup -> {LOCAL_BACKUP}")
    payload = {
        "_fetched_at": datetime.datetime.now().isoformat(timespec="seconds"),
        "_source": API_URL,
        "data": data,
    }
    with open(LOCAL_BACKUP, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
    print(f"      wrote {os.path.getsize(LOCAL_BACKUP)} bytes")


def upload_remote(data):
    print(f"[3/3] Uploading to {USER}@{HOST}:{REMOTE_PATH}")
    client = paramiko.SSHClient()
    client.load_host_keys(KNOWN_HOSTS)
    client.set_missing_host_key_policy(paramiko.RejectPolicy())
    client.connect(
        HOST, port=PORT, username=USER, key_filename=KEY_PATH,
        look_for_keys=False, allow_agent=False, timeout=20,
    )
    try:
        # Make sure the directory exists
        stdin, stdout, stderr = client.exec_command(
            "mkdir -p /opt/chuxi/data && ls -la /opt/chuxi/data/bangumi-calendar.json 2>/dev/null || true"
        )
        stdout.read()
        # Upload via temp file + atomic move
        tmp_remote = "/tmp/bangumi-calendar.upload.json"
        body = json.dumps(data, ensure_ascii=False)
        stdin, stdout, stderr = client.exec_command(
            f"cat > {tmp_remote} && mv -f {tmp_remote} {REMOTE_PATH} "
            f"&& echo OK && ls -la {REMOTE_PATH}"
        )
        stdin.write(body)
        stdin.channel.shutdown_write()
        out = stdout.read().decode(errors="replace").strip()
        err = stderr.read().decode(errors="replace").strip()
        if err:
            print(f"      [server stderr] {err}")
        print(f"      {out}")
        if "OK" not in out:
            raise RuntimeError("Upload verification failed")
    finally:
        client.close()
    print("      upload done")


def main():
    try:
        data = fetch_calendar()
        save_local(data)
        upload_remote(data)
        print("\n✓ Done. Frontend Calendar page should now show data (来源=服务器缓存).")
    except requests.exceptions.ProxyError as e:
        print(f"\n✗ 代理错误：{e}\n  请确认你的代理客户端已开启并监听 {PROXY or '(未配置)'}。", file=sys.stderr)
        sys.exit(2)
    except requests.exceptions.ConnectionError as e:
        print(f"\n✗ 网络错误：{e}", file=sys.stderr)
        sys.exit(2)
    except Exception as e:
        print(f"\n✗ 拉取失败：{type(e).__name__}: {e}", file=sys.stderr)
        print("  → 若代理正常仍失败（api.bgm.tv 被墙/限流），可临时用主项目方案重建：", file=sys.stderr)
        print("    python D:/workspace/网站/scripts/build_calendar_from_archive.py --out bangumi-calendar.archive.json", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
