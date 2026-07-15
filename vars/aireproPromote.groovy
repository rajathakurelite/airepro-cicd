/**
 * Stage → test gate → production promotion pipeline step.
 *
 * cfg: full service entry from services.yaml (slug, deploy, environments, test)
 */
def call(Map cfg) {
  stage('Deploy Stage') {
    aireproDeploy(cfg + [
      environment: 'stage',
      branch: cfg.environments?.stage?.branch ?: 'stage',
      health_url: cfg.environments?.stage?.health_url,
    ])
  }

  stage('Test on server.airepro.in') {
    aireproTest(cfg + [
      env: 'stage',
      api_base_url: cfg.environments?.stage?.api_base_url ?: cfg.test?.hire_api_base,
      areas: cfg.test?.promote_areas ?: [cfg.test?.area ?: 'hire'],
    ])
  }

  stage('Deploy Production') {
    aireproDeploy(cfg + [
      environment: 'production',
      branch: cfg.environments?.production?.branch ?: 'prod',
      health_url: cfg.environments?.production?.health_url,
    ])
  }
}
