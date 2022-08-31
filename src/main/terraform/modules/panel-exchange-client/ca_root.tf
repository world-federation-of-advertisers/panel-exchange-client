resource "aws_acmpca_certificate_authority_certificate" "root_ca_certificate" {
  certificate_authority_arn = aws_acmpca_certificate_authority.root_ca.arn

  certificate       = aws_acmpca_certificate.root_certificate.certificate
  certificate_chain = aws_acmpca_certificate.root_certificate.certificate_chain
}

resource "aws_acmpca_certificate" "root_certificate" {
  certificate_authority_arn   = aws_acmpca_certificate_authority.root_ca.arn
  certificate_signing_request = aws_acmpca_certificate_authority.root_ca.certificate_signing_request
  signing_algorithm           = "SHA256WITHECDSA"

  template_arn = "arn:${data.aws_partition.current.partition}:acm-pca:::template/RootCACertificate/V1"

  validity {
    type  = "DAYS"
    value = 365
  }
}

resource "aws_acmpca_certificate_authority" "root_ca" {
  type = "ROOT"

  certificate_authority_configuration {
    key_algorithm     = "RSA_2048"
    signing_algorithm = "SHA256WITHECDSA"

    subject {
      organization = var.resource_config.ca_org_name
      common_name = var.resource_config.ca_common_name
      # can add other parameters later
    }
  }
}
