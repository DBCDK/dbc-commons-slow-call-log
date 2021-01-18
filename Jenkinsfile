pipeline {
    agent { label "devel10" }
    tools {
        maven "maven 3.5"
    }
    environment {
        MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
    }
    stages {
        stage("build") {
            steps {
                script {
                    def status = sh returnStatus: true, script:  """
                        rm -rf \$WORKSPACE/.repo
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo dependency:resolve dependency:resolve-plugins >/dev/null 2>&1 || true
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo clean
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo --fail-at-end install -Dsurefire.useFile=false
                    """

                    // We want code-coverage and pmd/spotbugs even if unittests fails
                    status += sh returnStatus: true, script:  """
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo pmd:pmd pmd:cpd spotbugs:spotbugs javadoc:aggregate
                    """

                    junit testResults: '**/target/*-reports/TEST-*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc], failedTotalAll: 1

                    def pmd = scanForIssues tool: [$class: 'Pmd', pattern: '**/target/pmd.xml']
                    publishIssues issues:[pmd], failedTotalAll: 1

                    def cpd = scanForIssues tool: [$class: 'Cpd', pattern: '**/target/cpd.xml']
                    publishIssues issues:[cpd], failedTotalAll: 1

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs', pattern: '**/target/spotbugsXml.xml']
                    publishIssues issues:[spotbugs], failedTotalAll: 1

                    warnings consoleParsers: [
                         [parserName: "Java Compiler (javac)"],
                         [parserName: "JavaDoc Tool"]],
                         unstableTotalAll: "0",
                         failedTotalAll: "0"

                    if ( status != 0 ) {
                        error("Build failed")
                    }
                }
            }
        }

        stage("coverage") {
            steps {
                step([$class: 'JacocoPublisher', 
                      execPattern: '**/target/*.exec',
                      classPattern: '**/target/classes',
                      sourcePattern: '**/src/main/java',
                      exclusionPattern: '**/src/test*'
                ])
            }
        }

        stage("upload") {
            steps {
                script {
                    if (env.BRANCH_NAME == 'master') {
                        sh """
                            mvn -Dmaven.repo.local=\$WORKSPACE/.repo jar:jar deploy:deploy
                        """
                    }
                }
            }
        }
    }

    post {
        failure {
            script {
                if ("${env.BRANCH_NAME}" == 'master') {
                    emailext(
                            recipientProviders: [developers(), culprits()],
                            to: "os-team@dbc.dk",
                            subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} failed",
                            mimeType: 'text/html; charset=UTF-8',
                            body: "<p>The master build failed. Log attached. </p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                            attachLog: true,
                    )
                    slackSend(channel: 'search',
                            color: 'warning',
                            message: "${env.JOB_NAME} #${env.BUILD_NUMBER} failed and needs attention: ${env.BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')

                } else {
                    // this is some other branch, only send to developer
                    emailext(
                            recipientProviders: [developers()],
                            subject: "[Jenkins] ${env.BUILD_TAG} failed and needs your attention",
                            mimeType: 'text/html; charset=UTF-8',
                            body: "<p>${env.BUILD_TAG} failed and needs your attention. </p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                            attachLog: false,
                    )
                }
            }
        }
        success {
            step([$class: 'JavadocArchiver', javadocDir: 'target/site/apidocs', keepAll: false])
        }
    }
}
