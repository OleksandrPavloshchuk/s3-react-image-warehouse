#!/usr/bin/env bash

set -e

aws \
    --endpoint-url=http://localhost:4567 \
    lambda invoke \
    --function-name thumbnail-lambda \
    --cli-binary-format raw-in-base64-out \
    --function-name thumbnail-lambda \
    --payload '{"test":"hello"}' \
    /tmp/lambda-response.json

cat /tmp/lambda-response.json
echo