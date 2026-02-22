# Terraform이 사용할 변수 이름과 타입을 선언하는 파일
# 실제 값은 안 들어가고, 변수들의 목록만 정의
# 실제 값이 저장될 terraform.tfvars에서는 키(변수명) : 값(값) 형태로 데이터가 형성되고,
# 여기에서는 값의 타입을 지정해야 하며 값이 없을 경우 default값을 입력해둘 수 있다.
# 즉, 변수의 스키마를 정의하는 파일이다.
variable "aws_region" {
  description = "AWS 리전"
  type = string
  default = "ap-northeast-2"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입"
  type = string
  default = "t3.small"
}

variable "key_name" {
  description = "EC2 SSH 키 페어 이름"
  type = string
  default = "forcicd-key"
}

variable "my_ips" {
  description = "SSH 접속 허용 IP 목록"
  type = list(string) # 여러 IP를 허용하기 위해서 리스트로 선언
}

variable "app_ingress_cidrs" {
  description = "애플리케이션(8080) 접근 허용 CIDR 목록"
  type = list(string)
  default = ["0.0.0.0/0"]
}

variable "db_name" {
  description = "MySQL 데이터베이스 이름"
  type = string
}

variable "db_username" {
  description = "MySQL 사용자 이름"
  type = string
}

variable "db_password" {
  description = "MySQL 비밀번호"
  type = string
  sensitive = true # sensitive 옵션을 켜면 터미널 출력 시 값이 마스킹이 된다. (민감한 값은 켜서 보호하는 게 좋다.)
}
