pipeline {
    // Defines where the pipeline runs (must have Git and AWS CLI installed)
    agent any 
    
    // Environment variables for configuration
    environment {
        // 1. AWS Credentials ID from Jenkins -> Manage Credentials
        AWS_CRED_ID = 'aws-cred' 
        
        // 2. Your S3 Bucket Name
        S3_BUCKET_NAME = 'jnkin/jn'
        
        // 3. Your CloudFront Distribution ID (e.g., EXXXXXXXXXXXXX)
        CLOUDFRONT_DISTRO_ID = 'E3H5J8BMQF0AYA' 
        
        // 4. Directory containing your static files. Use '.' if files are in the repository root.
        BUILD_DIR = '.' 
    }

    stages {
        stage('1. Checkout Code') {
            steps {
                echo 'Pulling latest code from Git...'
                // Uses the SCM configuration defined in the Jenkins job settings
                checkout scm 
            }
        }
        
        stage('2. Deploy to S3') {
            steps {
                echo 'Starting deployment to S3...'
                // Use withAWS to securely inject credentials
                withAWS(credentials: env.AWS_CRED_ID) {
                    sh """
                    # The 'sync' command uploads new/changed files and deletes old ones (--delete)
                    # We exclude the .git folder to prevent unnecessary uploads/sync errors
                    aws s3 sync ${BUILD_DIR} s3://${S3_BUCKET_NAME} --delete --exclude '.git/*'
                    echo "S3 deployment to ${S3_BUCKET_NAME} complete."
                    """
                }
            }
        }

        stage('3. Invalidate CloudFront Cache') {
            steps {
                echo 'Sending CloudFront invalidation request...'
                withAWS(credentials: env.AWS_CRED_ID) {
                    sh """
                    # Forces CloudFront to clear cache for all paths (/*) so users see new content immediately
                    aws cloudfront create-invalidation --distribution-id ${CLOUDFRONT_DISTRO_ID} --paths "/*"
                    echo "CloudFront invalidation request sent for ${CLOUDFRONT_DISTRO_ID}."
                    """
                }
            }
        }
    }
}