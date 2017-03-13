package com.github.mgk.gradle

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.event.ProgressListener
import com.amazonaws.event.ProgressEvent
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.text.DecimalFormat


class S3Extension {
    String profile
    String role
    String region
    String bucket
}


abstract class S3Task extends DefaultTask {
    @Input
    String bucket

    static AmazonS3Client s3Client

    String getBucket() { bucket ?: project.s3.bucket }

    def getS3Client() {
        if (s3Client) {
            return s3Client
        }
        def profileCreds
        if (project.s3.profile) {
            logger.quiet("Using AWS credentials profile: ${project.s3.profile}")
            profileCreds = new ProfileCredentialsProvider(project.s3.profile)
        }
        else {
            profileCreds = new ProfileCredentialsProvider()
        }
        def creds = new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                profileCreds,
                new EC2ContainerCredentialsProviderWrapper()
        )

        if (project.s3.role) {
            logger.quiet("Assuming role: ${project.s3.role}")
            creds = new STSAssumeRoleSessionCredentialsProvider.Builder(
                    project.s3.role,
                    "s3task")
                    .withStsClient(
                        AWSSecurityTokenServiceClientBuilder
                                .standard()
                                .withCredentials(creds)
                                .build()
                        )
                    .build()
        }

        s3Client = new AmazonS3Client(creds)
        String region = project.s3.region
        if (region) {
            s3Client.region = Region.getRegion(Regions.fromName(region))
        }
        s3Client
    }
}


class S3Upload extends S3Task {
    @Input
    String key

    @Input
    String file

    @Input
    boolean overwrite = false

    @TaskAction
    def task() {
        if (s3Client.doesObjectExist(bucket, key)) {
            if (overwrite) {
                logger.quiet("S3 Upload ${file} → s3://${bucket}/${key} with overwrite")
                s3Client.putObject(bucket, key, new File(file))
            }
            else {
                logger.quiet("s3://${bucket}/${key} exists, not overwriting")
            }
        }
        else {
            logger.quiet("S3 Upload ${file} → s3://${bucket}/${key}")
            s3Client.putObject(bucket, key, new File(file))
        }
    }
}


class S3Download extends S3Task {
    String key
    String file
    String keyPrefix
    String destDir

    @TaskAction
    def task() {
        TransferManager tm = new TransferManager(s3Client)
        Transfer transfer

        // directory download
        if (keyPrefix != null) {
            logger.quiet("S3 Download recursive s3://${bucket}/${keyPrefix} → ${project.file(destDir)}/")
            transfer = tm.downloadDirectory(bucket, keyPrefix, project.file(destDir))
        }

        // single file download
        else {
            logger.quiet("S3 Download s3://${bucket}/${key} → ${file}")
            File f = new File(file)
            f.parentFile.mkdirs()
            transfer = tm.download(bucket, key, f)
        }

        def listener = new S3Listener()
        listener.transfer = transfer
        transfer.addProgressListener(listener)
        transfer.waitForCompletion()
    }

    class S3Listener implements ProgressListener {
        Transfer transfer

        DecimalFormat df = new DecimalFormat("#0.0")
        public void progressChanged(ProgressEvent e) {
            logger.info("${df.format(transfer.progress.percentTransferred)}%")
        }
    }
}


class S3Plugin implements Plugin<Project> {
    void apply(Project target) {
        target.extensions.create('s3', S3Extension)
    }
}
