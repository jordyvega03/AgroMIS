terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

# Rol IRSA generico para un service account de K8s
resource "aws_iam_role" "service_account" {
  name = "${var.name_prefix}-${var.service_name}-sa-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = var.oidc_provider_arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${var.oidc_provider_url}:sub" = "system:serviceaccount:${var.namespace}:${var.service_account_name}"
          "${var.oidc_provider_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy" "service_account" {
  count  = var.inline_policy != null ? 1 : 0
  name   = "inline"
  role   = aws_iam_role.service_account.id
  policy = var.inline_policy
}

resource "aws_iam_role_policy_attachment" "service_account" {
  for_each   = toset(var.managed_policy_arns)
  role       = aws_iam_role.service_account.name
  policy_arn = each.value
}
