#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
fetch_and_upload_bangumi_subjects.py
====================================
本地拉取本站番剧记录对应的 bgm.tv 详情（条目/剧集/角色），上传到服务器磁盘缓存。
服务器后端（BangumiSubjectService.fetch）会优先读这个缓存，
所以即使服务器无法访问 api.bgm.tv（GFW/IP 级拦截），番剧详情页也能正常显示
（人物、剧集列表、评分分布等在线增强数据）。

使用方法：
  1. 打开你本机的代理（Clash / v2rayN / ...），确保 HTTPS_PROXY 已设
  2. 可选：BGM_TOKEN=你的bgm令牌（提高限流配额）
  3. SSH_PWD=你的服务器密码 python fetch_and_upload_bangumi_subjects.py
  4. 新增番剧后重跑一次即可刷新缓存；也可加 Windows 任务计划定期跑

依赖：requests, paramiko（已安装在 C:\Users\zchux\.workbuddy\binaries\python\versions\3.13.12）
"""

import os
import sys
import json
import time
import getpass

import requests
import paramiko

# ---------- 配置 ----------
HOST = "106.14.202.90"
PORT = 22
USER = "root"
PASSWORD = os.environ.get("SSH_PWD") or getpass.getpass("Server SSH password: ")

# 本站番剧记录来源（取 subjectId 列表）：默认线上站点，也可用本地后端
RECORDS_URL = os.environ.get("RECORDS_URL", "https://www.chuxi.online/api/front/bangumi")
BGM_API = "https://api.bgm.tv"
BGM_TOKEN = os.environ.get("BGM_TOKEN", "")

# 与后端 BangumiSubjectService 的磁盘缓存目录 data/bangumi-bgm/ 对应
# （后端运行目录 /opt/chuxi -> 实际文件 /opt/chuxi/data/bangumi-bgm/{kind}-{sid}.json）
REMOTE_DIR = "/opt/chuxi/data/bangumi-bgm"
LOCAL_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "bangumi-bgm-local")

# 代理：依次尝试环境变量里的 HTTPS_PROXY / HTTP_PROXY / ALL_PROXY
PROXY = (
    os.environ.get("HTTPS_PROXY")
    or os.environ.get("https_proxy")
    or os.environ.get("HTTP_PROXY")
    or os.environ.get("http_proxy")
    or os.environ.get("ALL_PROXY")
)
PROXIES = {"http": PROXY, "https": PROXY} if PROXY else None

# 三种在线数据对应的 bgm 路径
KINDS = {
    "subject": lambda sid: f"/v0/subjects/{sid}",
    "episodes": lambda sid: f"/v0/episodes?subject_id={sid}&type=0&limit=100&offset=0",
    "characters": lambda sid: f"/v0/subjects/{sid}/characters",
}
# ---------- /配置 ----------


def get_subject_ids():
    print(f"[1/4] Fetching records from {RECORDS_URL}")
    resp = requests.get(RECORDS_URL, timeout=20)
    resp.raise_for_status()
    data = resp.json()
    items = data.get("data") if isinstance(data, dict) else data
    if not isinstance(items, list):
        raise ValueError(f"Unexpected payload: not a list (got {type(items).__name__})")
    ids = sorted({int(r["subjectId"]) for r in items if r.get("subjectId")})
    print(f"      OK -> {len(ids)} subjects: {ids[:20]}{'...' if len(ids) > 20 else ''}")
    return ids


def fetch_bgm(path):
    headers = {
        "User-Agent": "chuxi-web-fetch/1.0 (https://www.chuxi.online)",
        "Accept": "application/json",
    }
    if BGM_TOKEN:
        headers["Authorization"] = f"Bearer {BGM_TOKEN}"
    resp = requests.get(
        BGM_API + path,
        proxies=PROXIES,
        timeout=20,
        headers=headers,
    )
    resp.raise_for_status()
    return resp.json()


def save_local(sid, payloads):
    os.makedirs(LOCAL_DIR, exist_ok=True)
    for kind, data in payloads.items():
        with open(
            os.path.join(LOCAL_DIR, f"{kind}-{sid}.json"),
            "w",
            encoding="utf-8",
        ) as f:
            json.dump(data, f, ensure_ascii=False)
    print(f"      saved {sid} -> " + ", ".join(payloads.keys()))


def upload_remote():
    if not os.path.isdir(LOCAL_DIR):
        print("      nothing to upload")
        return
    files = sorted(os.listdir(LOCAL_DIR))
    print(f"[4/4] Uploading {len(files)} files to {USER}@{HOST}:{REMOTE_DIR}")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, port=PORT, username=USER, password=PASSWORD, timeout=20)
    try:
        stdin, stdout, stderr = client.exec_command(f"mkdir -p {REMOTE_DIR}")
        stdout.read()
        sftp = client.open_sftp()
        try:
            for name in files:
                local = os.path.join(LOCAL_DIR, name)
                remote = f"{REMOTE_DIR}/{name}"
                sftp.put(local, remote)
            print(f"      uploaded {len(files)} files")
        finally:
            sftp.close()
    finally:
        client.close()


def main():
    try:
        ids = get_subject_ids()
        if not ids:
            print("
✓ 没有可缓存的番剧 subjectId（先在后台上传/同步番剧记录）")
            return
        print(f"[2/4] Fetching bgm.tv details (proxy = {PROXY or '(direct)'})")
        for i, sid in enumerate(ids, 1):
            payloads = {}
            ok = True
            for kind, make_path in KINDS.items():
                try:
                    payloads[kind] = fetch_bgm(make_path(sid))
                except Exception as e:
                    print(f"      [{i}/{len(ids)}] {sid} {kind} FAILED: {type(e).__name__}: {e}")
                    ok = False
            if ok:
                save_local(sid, payloads)
                print(f"      [{i}/{len(ids)}] {sid} OK")
            else:
                # 部分失败仍保留成功的分片
                if payloads:
                    save_local(sid, payloads)
            time.sleep(0.6)  # 限流礼貌间隔
        upload_remote()
        print("
✓ Done. 番剧详情页现在应从服务器缓存读取（无需代理）。")
    except requests.exceptions.ProxyError as e:
        print(f"
✗ 代理错误：{e}
  请确认你的代理客户端已开启并监听 {PROXY or '(未配置)'}。", file=sys.stderr)
        sys.exit(2)
    except requests.exceptions.ConnectionError as e:
        print(f"
✗ 网络错误：{e}", file=sys.stderr)
        sys.exit(2)
    except Exception as e:
        print(f"
✗ 执行失败：{type(e).__name__}: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
