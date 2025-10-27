pipeline {
  agent any

  environment {
    S3_BUCKET = 'jnkin/jn'          // your S3 bucket name
    CLOUDFRONT_ID = 'E3H5J8BMQF0AYA'          // your CloudFront Distribution ID
    AWS_REGION = 'us-east-1'                // your AWS region
  }

  stages {
    stage('Deploy to S3') {
      steps {
        withAWS(credentials: 'aws-cred', region: "${AWS_REGION}") {
          // Upload all files to S3, remove old ones
          sh "aws s3 sync ./ s3://${S3_BUCKET} --exclude '.git/*' --delete"
        }
      }
    }

    stage('Invalidate CloudFront') {
      steps {
        withAWS(credentials: 'aws-cred', region: "${AWS_REGION}") {
          // Clear CloudFront cache
          sh "aws cloudfront create-invalidation --distribution-id ${CLOUDFRONT_ID} --paths '/*'"
        }
      }
    }
  }
}
