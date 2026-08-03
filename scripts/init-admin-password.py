#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
init-admin-password.py
======================
安全初始化管理员密码（fail-closed，不包含任何默认口令）。

用法：
    1. 设置数据库连接环境变量（与后端启动配置一致）：
         DB_URL      例如 jdbc:mysql://localhost:3306/chuxi_db?serverTimezone=Asia/Shanghai
         DB_USERNAME 例如 root
         DB_PASSWORD 数据库口令
       （可省略 DB_URL，缺省 localhost:3306/chuxi_db；pymysql 未安装时仅输出 SQL 供人工执行）
    2. 运行本脚本，交互式输入两次新口令（不回显，要求 >= 16 位）：

         python scripts/init-admin-password.py

    3. 脚本按后端 PasswordHasher 相同算法（PBKDF2WithHmacSHA256）生成哈希，
       只写入"密码记录不存在"时（WHERE NOT EXISTS），绝不覆写已有密码。

安全说明：
    * 本脚本与仓库不存储任何明文口令；
    * 记录已存在时脚本静默跳过（exit 0），如需重置请先在应用内修改密码；
    * 数据库口令仅从环境变量读取，不落盘、不打日志。
"""

import base64
import getpass
import hashlib
import os
import re
import secrets
import sys

# 与 backend PasswordHasher.java 保持一致
ITERATIONS = 120_000
KEY_LENGTH_BYTES = 32  # Java PBEKeySpec keyLength=256 bit

MIN_PASSWORD_LENGTH = 16


def pbkdf2_hash(password: str, salt: bytes, iterations: int = ITERATIONS) -> bytes:
    return hashlib.pbkdf2_hmac(
        "sha256", password.encode("utf-8"), salt, iterations, dklen=KEY_LENGTH_BYTES
    )


def format_hashed(password: str, iterations: int = ITERATIONS) -> str:
    salt = secrets.token_bytes(16)
    dk = pbkdf2_hash(password, salt, iterations)
    enc = base64.b64encode
    return f"pbkdf2${iterations}${enc(salt).decode('ascii')}${enc(dk).decode('ascii')}"


def parse_db_url(db_url: str):
    """jdbc:mysql://host:port/db?params -> (host, port, db)"""
    m = re.match(r"jdbc:mysql://([^:/?]+)(?::(\d+))?/([^?]+)", db_url or "")
    if not m:
        return "127.0.0.1", 3306, "chuxi_db"
    host = m.group(1)
    port = int(m.group(2) or 3306)
    db = m.group(3)
    return host, port, db


def build_sql(hashed: str) -> str:
    return (
        "INSERT INTO site_content (content_key, content_json, updated_at)\n"
        "SELECT 'admin-password', "
        + "'{\"password\":\"" + hashed + "\"}', NOW()\n"
        "WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE content_key = 'admin-password');\n"
    )


def main() -> int:
    p1 = getpass.getpass("新管理员密码（>= %d 位，不回显）: " % MIN_PASSWORD_LENGTH)
    p2 = getpass.getpass("再次输入确认: ")
    if p1 != p2:
        print("错误：两次输入不一致", file=sys.stderr)
        return 1
    if len(p1) < MIN_PASSWORD_LENGTH:
        print("错误：密码长度不足 %d 位" % MIN_PASSWORD_LENGTH, file=sys.stderr)
        return 1

    hashed = format_hashed(p1)
    sql = build_sql(hashed)

    host, port, db = parse_db_url(os.environ.get("DB_URL", ""))
    username = os.environ.get("DB_USERNAME", "")
    password = os.environ.get("DB_PASSWORD", "")
    if not username:
        print("提示：未设置 DB_USERNAME，改为仅输出 SQL 供人工执行。", file=sys.stderr)
        print(sql)
        return 0

    try:
        import pymysql  # type: ignore
    except ImportError:
        print("提示：未安装 pymysql，仅输出 SQL 供人工执行（pip install pymysql 后可自动执行）。",
              file=sys.stderr)
        print(sql)
        return 0

    conn = None
    try:
        conn = pymysql.connect(host=host, port=port, user=username, password=password,
                               database=db, charset="utf8mb4")
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM site_content WHERE content_key = 'admin-password'")
            exists = cur.fetchone() is not None
        if exists:
            print("管理员密码已存在，跳过（绝不覆写已有密码）。")
            return 0
        with conn.cursor() as cur:
            cur.execute(sql)
        conn.commit()
        print("管理员密码已初始化（PBKDF2 哈希写入，记录原本不存在）。")
        return 0
    except Exception as e:  # noqa: BLE001 - 统一给出可执行 SQL，避免脚本半途失败
        print("数据库写入失败：%s" % e, file=sys.stderr)
        print("请人工执行以下 SQL（已按本脚本生成，含哈希，不含明文）：\n%s" % sql)
        return 2
    finally:
        if conn is not None:
            conn.close()


if __name__ == "__main__":
    sys.exit(main())
