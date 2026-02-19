# EC2가 SSM 명령을 받을 수 있도록 IAM 역할과 권한을 설정하는 파일
# IAM 역할 - EC2한테 "너는 SSM 명령을 받아도 돼"라는 권한 부여

# SSM;Simple Systems Manager : AWS가 EC2에 원격으로 명령을 보낼 수 있는 서비스.
#                              여기에서는 GitHub Actions -> AWS -> EC2에 배포 명령 전달하는 용도로 사용.
#                              현재는 AWS Systems Manager로 명칭이 바뀌었는데, 그냥 SSM이라 부름.

# Terraform 적용 이전 : GitHub Actions -> SSH -> EC2 (self-hosted runner가 EC2 메모리 위에 항상 상주(약 200MB))
# Terraform 적용 이후 : SSM Send-Command -> EC2 (상주하는 runner 없음), Ubuntu AMI에 기본 내장된 SSM Agent가 필요 명령을 순차적으로 알아서 실행해가며 CICD

# ========= EC2가 SSM 명령을 받을 수 있도록 IAM 역할 정의 ==========
resource "aws_iam_role" "ec2_role" {
  name = "forcicd-ec2-role"

  # assume_role_policy : '이 역할을 EC2 서비스가 사용할 수 있다'는 신뢰 정책. AWS IAM 정책 문법은 JSON으로 작성되어야 함.
  assume_role_policy = jsonencode({
    Version = "2012-10-17" # AWS가 2012년 10월에 대편 개정된 IAM 정책 작성 문법을 쭉 사용 중. 문법의 버전임. 정책 자체가 아님. 앞으로도 쭉 사용할 걸로 예상됨.(이건 항상 고정이라 생각해도 됨)
    # Statement : 이 정책이 허용/거부하는 규칙 목록
    Statement = [
      {
        # Action : 허용/거부할 행동 지정
        # - sts(security token service): AWS에서 임시 자격증명을 발급하는 서비스.
        # - AssumeRole : EC2가 이 역할을 사용하겠다고 요청하는 행동
        Action = "sts:AssumeRole"

        # Effect : 이 규칙이 허용인지 거부인지
        Effect = "Allow"

        # Principal : 이 역할을 사용할 수 있는 사용자
        Principal = {
          Service = "ec2.amazonaws.com" # EC2서비스만 이 역할을 사용 가능. AWS 서비스 자체를 대상으로 지정.
        }
        # 만약 특정 IAM 유저한테 허용하고 싶다면 Principal을 아래 형식으로 작성
        # Principal = {
        #   AWS = "arn:aws:iam::1234566789012:user/forcicd-terraform"
        # }
      }
    ]
  })
}

# ========== SSM 권한 정책을 IAM 역할에 연결 ==========
resource "aws_iam_role_policy_attachment" "ssm_policy" {
  role = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore" # AWS가 제공하는 권한 모음 정책
}

# ========== EC2에 IAM 역할을 붙이기 위한 인스턴스 프로필 ==========
# - 인스턴스 프로필 : IAM 역할을 직접 EC2에 붙일 수 없어서 프로필이라는 래퍼가 필요. main.tf의 EC2블록에서 iam_instance_profile에 사용됨.
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "forcicd-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

