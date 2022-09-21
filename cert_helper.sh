#!/bin/bash
set -e

# request certificate
#aws acm request-certificate \
#--domain-name www.example.com \
#--validation-method DNS \
#--idempotency-token 1234 \
#--options CertificateTransparencyLoggingPreference=DISABLED

# get certificate pieces
aws acm export-certificate \
     --certificate-arn arn:aws:acm:us-west-1:010295286036:certificate/5ff282b6-bae0-4f3c-adaf-94a0bc3b26fb \
     --passphrase fileb://passphrase.txt \
     --output json \
     | jq -r '"\(.Certificate)"' \
     > src/main/k8s/testing/secretfiles/mpx_tls.pem

aws acm export-certificate \
     --certificate-arn arn:aws:acm:us-west-1:010295286036:certificate/5ff282b6-bae0-4f3c-adaf-94a0bc3b26fb \
     --passphrase fileb://passphrase.txt \
     --output json \
     | jq -r '"\(.PrivateKey)"' \
     > src/main/k8s/testing/secretfiles/mpx_tls.enc.key


openssl pkcs8 -in src/main/k8s/testing/secretfiles/mpx_tls.enc.key -out mpx_tls.key
