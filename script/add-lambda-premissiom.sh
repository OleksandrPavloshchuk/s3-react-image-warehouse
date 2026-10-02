#!/usr/bin/env bash

set -e

LAMBDA_NAME="thumbnail-lambda"
ENDPOINT="http://localhost:4567"
BUCKET=image-warehouse

aws --endpoint-url="$ENDPOINT" \
    s3api put-bucket-notification-configuration \
    --bucket "$BUCKET" \
    --notification-configuration file://../docker/notification.json

if aws --endpoint-url="$ENDPOINT" lambda get-policy --function-name "$LAMBDA_NAME"; then
  echo "Policy exists."
else
      aws --endpoint-url="$ENDPOINT" \
          lambda add-permission \
          --function-name "$LAMBDA_NAME" \
          --statement-id s3-invoke \
          --action lambda:InvokeFunction \
          --principal s3.amazonaws.com \
          --source-arn "arn:aws:s3:::$BUCKET"
      echo "Policy is created."
fi