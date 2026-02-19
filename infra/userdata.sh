#!/bin/bash

# EC2 초기 구동 시 자동 실행되는 스크립트
# Docker 설치 + 앱 디렉토리 + .env 파일 준비

# 이 스크립트 실행 중 명령어가 하나라도 실패하면 즉시 중단하는 옵션
set -e

# 패키지 업데이트
apt-get update -y

# Docker 설치에 필요한 의존성 (GPG 서명확인)
apt-get install -y ca-certificates curl gnupg lsb-release

# Docker 공식 GPG 키 추가
install -m 0755 -d /etc/apt/keyrings

curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
-o /etc/apt/keyrings/docker.asc

chmod a+r /etc/apt/keyrings/docker.asc

# Docker apt 레포지토리 추가
echo \
"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu \
$(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
| tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker 설치
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# ubuntu 유저가 sudo 없이 docker 사용 가능하도록 설정
usermod -aG docker ubuntu

# 앱 디렉토리 생성
mkdir -p /home/ubuntu/app
chown ubuntu:ubuntu /home/ubuntu/app

# .env 파일 생성 (Terraform 변수에서 주입)
cat > /home/ubuntu/app/.env << 'EOF'
DB_URL=jdbc:mysql://db:3306/${db_name}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=${db_username}
DB_PASSWORD=${db_password}
EOF

chown ubuntu:ubuntu /home/ubuntu/app/.env

# Docker 서비스 시작 및 부팅 시 자동 시작 설정
systemctl enable docker
systemctl start docker

