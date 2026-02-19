# Terraform은 AWS, GCP, Azure 등 많은 클라우드를 지원한다.
# 어떤 클라우드 서비스(Terraform provider)를, 어느 리전에서, 어떤 버전으로 등의 설정정보를 선언해줘야 한다.

terraform {
  required_version = ">= 1.5.0" # Terraform v1.5.0 이상
  required_providers {  # 사용할 provider 목록
    aws = {  # aws
      source = "hashicorp/aws" # hashicorp는 Terraform 개발사, 공식 지원하는 aws플러그인 사용
      version = "~> 5.0" # aws provider v5.0이상 v6.0 미만 버전으로만 실행 가능
    }
  }
}

# 위에서 설정한 provider 중 aws에 대한 설정 블록
provider "aws" {
  region = var.aws_region # variables.tf에서 변수 선언, 실제 값은 terraform.tfvars에서 입력
}