/**
 * Deploy airepro-stage (or other dual_folder service) to stage or production.
 *
 * cfg keys:
 *   slug, deploy (git_url, frontend_dir, backend_dir, scripts, credentials_id)
 *   environment: 'stage' | 'production'
 *   branch: override branch name
 */
def call(Map cfg) {
  def envName = cfg.environment ?: 'stage'
  def branch = cfg.branch ?: (envName == 'production' ? 'prod' : 'stage')
  def gitUrl = cfg.deploy?.git_url
  def credId = cfg.deploy?.credentials_id ?: 'eliteprofessional'
  def backendScript = cfg.deploy?.deploy_backend_script ?: 'backend/scripts/docker_deploy.sh'
  def frontendScript = cfg.deploy?.deploy_frontend_script ?: 'hireFrontend/deploy-docker.sh'
  def backendDir = cfg.deploy?.backend_dir ?: 'backend'
  def frontendDir = cfg.deploy?.frontend_dir ?: 'hireFrontend'
  def healthUrl = cfg.environments?."${envName}"?.health_url ?: cfg.health_url

  stage("Checkout ${cfg.slug ?: 'service'} @ ${branch}") {
    checkout([
      $class: 'GitSCM',
      branches: [[name: "*/${branch}"]],
      doGenerateSubmoduleConfigurations: false,
      extensions: [
        [$class: 'CloneOption', depth: 1, shallow: true, noTags: true],
        [$class: 'CleanBeforeCheckout'],
      ],
      submoduleCfg: [],
      userRemoteConfigs: [[url: gitUrl, credentialsId: credId]],
    ])
  }

  stage("Deploy backend (${envName})") {
    sh """
      set -e
      cd '${backendDir}'
      chmod +x scripts/*.sh 2>/dev/null || true
      bash ${backendScript.replaceFirst("^${backendDir}/", '')}
    """
  }

  stage("Deploy frontend (${envName})") {
    sh """
      set -e
      cd '${frontendDir}'
      chmod +x *.sh 2>/dev/null || true
      bash ${frontendScript.replaceFirst("^${frontendDir}/", '')} '${branch}'
    """
  }

  if (healthUrl) {
    stage('Verify health') {
      sh """
        set -e
        echo "Checking ${healthUrl}"
        for i in 1 2 3 4 5; do
          if curl -sf '${healthUrl}' > /dev/null; then
            echo "Health check passed"
            exit 0
          fi
          echo "Attempt \$i failed, retrying..."
          sleep 10
        done
        echo "Health check failed after retries"
        exit 1
      """
    }
  }
}
