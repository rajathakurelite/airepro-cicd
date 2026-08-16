/**
 * Clone airepro_cicd and run Playwright tests for one or more areas.
 *
 * cfg keys:
 *   test.area or areas (list)
 *   env: 'stage' | 'production' (default stage)
 *   test_repo: git_url, branch, credentials_id
 *   api_base_url: optional BASE_URL override
 */
def call(Map cfg) {
  def testRepo = cfg.test_repo ?: [
    git_url: 'https://github.com/eliteprofessional/airepro_cicd.git',
    branch: 'fixes_ai',
    credentials_id: 'github-rajathakur',
  ]
  def envName = cfg.env ?: cfg.environment ?: 'stage'
  def areas = cfg.areas ?: (cfg.test?.promote_areas ?: [cfg.test?.area ?: 'hire'])
  def apiOverride = cfg.api_base_url ?: cfg.test?.hire_api_base ?: ''

  def areaPaths = [
    hire: 'src/tests/hire',
    security: 'src/tests/security',
    ems: 'src/tests/EMS_service',
    obo: 'src/tests/OBO',
    email: 'src/tests/email',
    blogging: 'src/tests/blogging',
    lernify: 'src/tests/lernify',
    timesheet: 'src/tests/timesheet',
    feature_flag: 'src/tests/feature_flag',
    other: 'src/tests/other',
  ]

  stage('Checkout airepro_cicd') {
    dir('airepro_cicd') {
      checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${testRepo.branch}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [
          [$class: 'CloneOption', depth: 1, shallow: true, noTags: true],
          [$class: 'CleanBeforeCheckout'],
        ],
        submoduleCfg: [],
        userRemoteConfigs: [[
          url: testRepo.git_url,
          credentialsId: testRepo.credentials_id ?: 'github-rajathakur',
        ]],
      ])
    }
  }

  stage('Install test dependencies') {
    dir('airepro_cicd') {
      sh '''
        set -e
        . scripts/ci-setup-node.sh
        npm ci
        # Hire/API Playwright tests do not need Chromium OS packages.
        # install-deps uses sudo and fails this Jenkins agent (test-hire #6/#7).
      '''
    }
  }

  stage("Test on ${envName}") {
    dir('airepro_cicd') {
      def defaultBaseUrl = envName == 'production'
        ? 'https://vps.airepro.in/api/v1'
        : 'https://prodhirebe.airepro.in/api/v1'
      def baseUrl = apiOverride?.trim() ?: defaultBaseUrl

      for (def area : areas) {
        if (!area) continue
        def testPath = areaPaths[area] ?: "src/tests/${area}"
        def hireTarget = area == 'hire' ? 'hire' : 'other'
        def runExternal = !['hire', 'security'].contains(area)
        sh """
          set -e
          . scripts/ci-setup-node.sh
          export CI=true
          export BASE_URL='${baseUrl}'
          export HIRE_API_TARGET='${hireTarget}'
          export RUN_EXTERNAL_TESTS='${runExternal}'
          npx playwright test ${testPath}
        """
      }
    }
  }

  stage('Archive test reports') {
    dir('airepro_cicd') {
      archiveArtifacts artifacts: 'playwright-report/**,test-results/**', allowEmptyArchive: true
    }
  }
}
