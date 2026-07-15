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
    credentials_id: 'eliteprofessional',
  ]
  def envName = cfg.env ?: cfg.environment ?: 'stage'
  def areas = cfg.areas ?: (cfg.test?.promote_areas ?: [cfg.test?.area ?: 'hire'])
  def apiOverride = cfg.api_base_url ?: cfg.test?.hire_api_base ?: ''

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
          credentialsId: testRepo.credentials_id ?: 'eliteprofessional',
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
        npx playwright install-deps
      '''
    }
  }

  stage("Test on ${envName}") {
    dir('airepro_cicd') {
      for (def area : areas) {
        if (!area) continue
        def apiFlag = (apiOverride?.trim()) ? "--api-base-url=${apiOverride.trim()}" : ''
        sh """
          set -e
          . scripts/ci-setup-node.sh
          export CI=true
          node scripts/run-service-tests.js --area=${area} --env=${envName} ${apiFlag}
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
