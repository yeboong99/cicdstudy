# terraform apply 완료 후 터미널에 출력할 값들을 정의하는 파일.
# 인프라 생성 후 바로 필요한 정보 (IP, 인스턴스 ID, SSH 명령어 등)를 편하게 확인할 수 있음.

# ========== EC2에 고정된 공인IP 정보 ==========
output "elastic_ip" {
  description = "EC2 고정 공인 IP"
  value = aws_eip.main.public_ip
}

# ========== EC2 인스턴스 ID 정보 ==========
output "instance_id" {
  description = "EC2 인스턴스 ID (Github Secrets에 등록 필요)"
  value = aws_instance.main.id
}

# ========== SSH 접속 명령어 안내 ==========
output "ssh_command" {
  description = "SSH 접속 명령어"
  value = "ssh -i ~/.ssh/forcicd-key.pem ubuntu@${aws_eip.main.public_ip}"
}

# ========== 애플리케이션 접속 URL 안내 ==========
output "app_url" {
  description = "애플리케이션 접속 URL"
  value = "http://${aws_eip.main.public_ip}:8080"
}