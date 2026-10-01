#!/usr/bin/env bash

set -x

AWS_ENDPOINT="http://localhost:4567"
BUCKET_NAME="image-warehouse"

aws --endpoint-url "$AWS_ENDPOINT" s3 rm "s3://$BUCKET_NAME" --recursive

echo "Bucket emptied: $BUCKET_NAME"