#!/usr/bin/env bash

set -x

AWS_ENDPOINT="http://localhost:4567"
ROOT_BUCKET_NAME="image-warehouse"

BUCKET_NAME=""

createBucket() {
  aws --endpoint-url "${AWS_ENDPOINT}" s3 mb "s3://${BUCKET_NAME}"
  echo "Created bucket: ${BUCKET_NAME}"
}

BUCKET_NAME="${ROOT_BUCKET_NAME}"
createBucket
