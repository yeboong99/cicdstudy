# VPC, 서브넷, 인터넷 게이트웨이, 보안 그룹, Elastic IP, EC2 인스턴스를 정의하는 파일
# Terraform에서 모든 인프라 자원은 resource 키워드로 선언한다.

# 블록 형식
# resource "리소스_타입" "내부_이름" {
#   설정_이름 = 값
# }

# ========== VPC ==========
# "aws_vpc" : AWS의 VPC를 만들겠다는 타입. AWS provider가 제공하는 타입 이름.
# "main"    : Terraform 코드 안에서만 쓰는 별명. 다른 파일에서 이 VPC를 참조할 때 aws_vpc.main 뒤에 .id라는 키워드를 붙여 쓰게 된다. AWS콘솔에 표시되는 이름과는 별개.
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"  # 이 VPC가 사용할 IP 주소 범위. 10.0.0.0 ~ 10.0.255.255 앞 16비트 고정, 뒤 16비트 변경으로 사용 가능한 IP 범위로 (65,536개) 사용하겠다는 의미.
  enable_dns_hostnames = true # DNS 호스트네임을 자동으로 부여할지 여부. SSM이 내부적으로 이 DNS를 사용하기 때문에 true로 설정해야 함. 찾아줄 때 이 dns support(dns resolution(조회)) 기능을 사용함.
  enable_dns_support = true # DNS_hostnames가 동작하려면 반드시 세트로 true여야 함. 찾을 때

  tags = {
    Name = "forcicd-vpc" # AWS 리소스에 붙이는 라벨(태그). AWS 콘솔에서는 이 이름으로 표시된다. 이게 없으면 전부 ID로만 표시되어서 리소스를 구분하기가 힘들다.
  }
}

# ========== 퍼블릭 서브넷 ==========
# "aws_subnet" : AWS 서브넷을 만들겠다는 타입
# "public" : 코드 내 별명. 다른 곳에서 aws_subnet.public.id로 참조
resource "aws_subnet" "public" {
  vpc_id = aws_vpc.main.id # 이 서브넷이 어느 VPC의 서브넷인지 지정
  cidr_block = "10.0.1.0/24" # 이 서브넷이 사용할 IP 범위. VPC가 사용중인 IP 범위 중 일부를 떼어준다. 여기서는 10.0.1.0 ~ 10.0.1.255 (256개) (앞 24비트만 고정)
  availability_zone = "${var.aws_region}a" # 이 서브넷의 가용 영역 지정. 서울 리전(ap-northeast-2)의 물리적 데이터센터 위치(실제로 a, b, c, d 4곳이 있음) 중 a 사용.
  map_public_ip_on_launch = true # EC2 설정 시 별도 설정 없이도 인터넷에서 접근가능한 공인 IP를 자동으로 붙여주는 옵션. 사실 EC2에 고정 IP를 붙여줄 거라서 필수는 아니지만 관례적으로 퍼블릭 서브넷을 설정할 때에는 켜두는 게 좋다.

  tags = {
    Name = "forcicd-subnet-public" # AWS 퍼블릭 서브넷에 붙일 이름. AWS 콘솔에는 이 이름으로 표시된다. 위 블럭의 태그(라벨) 설정과 같은 이유.
  }
}

# ========== 서브넷의 인터넷 게이트웨이 ==========
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id # 어느 VPC의 게이트웨이인지 지정

  tags = {
    Name = "forcicd-igw"
  }
}

# ========== 서브넷의 라우팅 테이블 ==========
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0" # 모든 목적지 IP에 대해
    gateway_id = aws_internet_gateway.main.id # 이 게이트웨이로 내보내라
  }

  tags = {
    Name = "forcicd-rt-public"
  }

}

# ========== 라우팅 테이블을 서브넷에 연결 ==========
resource "aws_route_table_association" "public" {
  subnet_id = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# ========== 보안 그룹 설정 ==========
resource "aws_security_group" "main" {
  name = "forcicd-sg"
  description = "forcicd security group"
  vpc_id = aws_vpc.main.id # 이 보안 그룹 설정을 사용할 vpc

  # ingress = 인바운드, egress = 아웃바운드
  ingress {
    description = "SSH"
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = var.my_ips # SSH 연결을 시도할 IP 목록
  }

  ingress {
    description = "App"
    from_port = 8080
    to_port = 8080
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0 # 모든 포트
    to_port = 0
    protocol = "-1" # 모든 프로토콜
    cidr_blocks = ["0.0.0.0/0"] # 목적지 전세계 전부 허용
  }

  tags = {
    Name = "forcicd-sg"
  }
}

# ========== Elastic IP ==========
resource "aws_eip" "main" {
  domain = "vpc" # 이 고정 아이피를 VPC안에서 사용한다는 의미.
  # - 특정 VPC(이 파일 상단의 VPC에서 사용하겠다는 의미 아님! VPC에 포함된 단말에 사용하는 EIP라는 이야기임.)
  # - 예전에 EC2-Classic이라는 VPC 없이 쓰던 방식이 있었는데, 그 때는 domain = "standard"라는 설정을 사용했었음. 지금은 EC2-Classic이 폐지되어서 항상 "vpc"로 사용함.
  tags = {
    Name = "forcicd-eip"
  }
}

# ========== EC2 ==========
resource "aws_instance" "main" {
  ami = "ami-0c9c942bd7bf113a2" # AWS 서울 리전의 ubuntu 22.04 LTS 공식 ami
  instance_type = var.instance_type # EC2 사양
  key_name = var.key_name # ssh 접속에 사용할 키 페어 이름 - variables.tf에 default값 정의되어 있음
  subnet_id = aws_subnet.public.id # 위에서 정의한 서브넷에 포함시킨다
  vpc_security_group_ids = [aws_security_group.main.id] # 위에서 정의한 보안 그룹 사용
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name # EC2에 부여할 IAM 역할 - iam.tf의 ec2_profile을 연결.

  # EC2 첫 부팅 시 딱 한번 자동 실행되는 스크립트. infra/userdata.sh를 읽어옴 ( Docker 설치, .env 파일 생성 등 스크립트가 담김 )
  user_data = templatefile("${path.module}/userdata.sh", {
    db_name = var.db_name
    db_username = var.db_username
    db_password = var.db_password
  })

  # EBS 설정
  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = {
    Name = "forcicd-ec2"
  }
}

# ========== Elastic IP와 EC2 연결 ==========
resource "aws_eip_association" "main" {
  instance_id = aws_instance.main.id # EIP 사용할 인스턴스
  allocation_id = aws_eip.main.id # 할당할 EIP (위에서 생성한 EIP ID)
}
