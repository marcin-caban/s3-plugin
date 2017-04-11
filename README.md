# S3 Gradle Plugin
[![Build Status](https://img.shields.io/travis/mgk/s3-plugin.svg)](https://travis-ci.org/mgk/s3-plugin)
[![Install](https://img.shields.io/badge/install-plugin-brown.svg)](https://plugins.gradle.org/plugin/com.github.mgk.gradle.s3)
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE)
![Flux Cap](https://img.shields.io/badge/flux%20capacitor-1.21%20GW-orange.svg)

Gradle plugin that uploads and downloads S3 objects.

## Setup

New way:

```groovy
plugins {
    id "com.github.mgk.gradle.s3" version "1.5.0"
}
```

Old way:

```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "gradle.plugin.com.github.mgk.gradle:s3:1.5.0"
    }
}

apply plugin: "com.github.mgk.gradle.s3"
```

## Versioning

This project uses [semantic versioning](http://semver.org)

See [gradle plugin page](https://plugins.gradle.org/plugin/com.github.mgk.gradle.s3) for other versions.

# Usage

## Authentication

The S3 plugin searches for credentials in the same order as the [AWS default credentials provider chain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html). Additionally you can specify a credentials profile to use by setting the project `s3.profile` property:

```groovy
s3 {
    profile = 'my-profile'
}
```

You can also specify a role to assume by setting the project `s3.role` property, e.g.:

```groovy
s3 {
    role = 'arn:aws:iam::000000000000:role/system/my-role'
}
```

Setting the environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` is one way to provide your S3 credentials. See the [AWS Docs](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) for details on credentials.

## Tasks

The following Gradle tasks are provided. See [the tests](example/build.gradle) for
examples.


### S3Upload

Upload a file to S3. Properties:

  + `bucket` - S3 bucket to use *(optional, defaults to the project `s3` configured bucket)*
  + `file` - path of file to be uploaded
  + `key` - key of S3 object to create.
  + `overwrite` - *(optional, default is `false`)*, if `true` the S3 object is created or overwritten if it already exists.

By default `S3Upload` does not overwrite the S3 object if it already exists. Set `overwrite` to `true` to upload the file even if it exists.

### S3Download

Downloads one or more S3 objects. This task has two modes of operation: single file
download and recursive download. Properties that apply to both modes:

  + `bucket` - S3 bucket to use *(optional, defaults to the project `s3` configured bucket)*

For a single file download:

  + `key` - key of S3 object to download
  + `file` - local path of file to save the download to

For a recursive download:

  + `keyPrefix` - S3 prefix of objects to download
  + `destDir` - local directory to download objects to

***Nota Bene***: recursive downloads create a sparse directory tree
containing the full `keyPrefix` under `destDir`. So with an S3 bucket
containing the object keys:

```
top/foo/bar
top/README
```

a recursive download:

```groovy
task downloadRecursive(type: S3Download) {
  keyPrefix = "top/foo/"
  destDir = "local-dir"
}
```

results in this local tree:

```
local-dir/
└── foo
    └── bar
```

So only files under `top/foo` are downloaded, but their full S3 paths are appended to the `destDir`. This is different from the behavior of the aws cli `aws s3 cp --recursive` command which prunes the root of the downloaded objects. Use the flexible [Gradle Copy](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Copy.html) task to prune the tree after downloading it. See `example/build.gradle` for an example.

## Progress Reporting

Downloads report percentage progress at the gradle INFO level. Run gradle with the `-i` option to see download progress.

## Development Notes

The `uploadArchives` task deploys to a local file maven repo under the build
directory. It also writes the current build to `build/VERSION`. The file
`example/build.gradle` serves as tests and doc. The tests use the local build
of the plugin.

The tests use a generated unique path in a test bucket to exercise all
of the plugins features.

The test bucket has a [AWS Object Expiration](https://aws.amazon.com/blogs/aws/amazon-s3-object-expiration/) policy that removes objects older
than one day automatically, so yay, no cleanup required.

The automated build uses an IAM access key that allows `listBucket`, `getObject`,
and `putObject` on the test bucket only. The AWS credits configured as Travis
environment variables. The access key id is not a secret so it is shown in the
logs, while the secret access key is secret and not shown.

## License
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE)
