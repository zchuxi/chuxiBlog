import os, paramiko, sys, time
from datetime import datetime

HOST = "106.14.202.90"
PORT = 22
USER = "root"
PASS = os.environ["SSH_PWD"]

JAR_LOCAL = r"D:/build/chuxi-backend2/target/chuxi-backend.jar"
DIST_TGZ = r"D:/workspace/dist_build.tgz"

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
    print("CMD> ", cmd)
    if out.strip():
        print(out)
    if err.strip():
        print("[ERR]", err)
    return out

ts = datetime.now().strftime("%Y%m%d%H%M%S")

# 1. 备份旧 jar
print("=== 1. 备份旧 jar ===")
run(f"mkdir -p /opt/chuxi/backup && cp -f /opt/chuxi/chuxi-backend.jar /opt/chuxi/backup/chuxi-backend.jar.bak.{ts} && echo backed_up")

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
#    本地打包命令：cd <dist父目录> && tar czf /tmp/dist.tgz dist
print("=== 4. 解压 dist ===")
run("rm -rf /opt/chuxi/dist && mkdir -p /opt/chuxi/dist && tar xzf /tmp/dist.tgz -C /opt/chuxi/ && echo dist_extracted && ls /opt/chuxi/dist | head")

# 5. 重启 chuxi
print("=== 5. 重启 chuxi.service ===")
run("systemctl restart chuxi")
print("... 等待 Spring Boot 启动 (含 Flyway 迁移) ...")
time.sleep(25)
run("systemctl is-active chuxi")
print("=== 启动日志尾部 ===")
run("journalctl -u chuxi -n 40 --no-pager", timeout=30)

s.close()
ssh.close()
t.close()
print("DONE")
