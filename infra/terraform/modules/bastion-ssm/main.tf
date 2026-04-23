terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

# Rol IAM para la instancia SSM (sin acceso SSH publico)
resource "aws_iam_role" "ssm" {
  name = "${var.name_prefix}-ssm-bastion-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ssm.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ssm" {
  name = "${var.name_prefix}-ssm-bastion-profile"
  role = aws_iam_role.ssm.name
}

resource "aws_security_group" "ssm" {
  name        = "${var.name_prefix}-ssm-bastion-sg"
  description = "SSM bastion — sin reglas de ingress (SSM no necesita SSH)"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS para SSM endpoint"
  }

  tags = var.common_tags
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

resource "aws_instance" "ssm" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t4g.nano"
  subnet_id              = var.private_subnet_id
  iam_instance_profile   = aws_iam_instance_profile.ssm.name
  vpc_security_group_ids = [aws_security_group.ssm.id]

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"  # IMDSv2
    http_put_response_hop_limit = 1
  }

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-ssm-bastion" })
}
