// Job DSL seed — generates Airepro folder jobs from services.yaml
// Run via: Airepro/seed-jobs (Process Job DSLs → seed/seedJob.groovy)

def testAreas = [
  [id: 'hire', label: 'Hire'],
  [id: 'security', label: 'Security'],
  [id: 'ems', label: 'EMS'],
  [id: 'obo', label: 'OBO'],
  [id: 'email', label: 'Email'],
  [id: 'blogging', label: 'Blogging'],
  [id: 'lernify', label: 'Lernify'],
  [id: 'timesheet', label: 'Timesheet'],
  [id: 'feature_flag', label: 'Feature Flag'],
]

folder('Airepro') {
  description('Airepro CI/CD — deploy, test, release')
}

folder('Airepro/test') {
  description('Playwright tests from airepro_cicd')
}

folder('Airepro/deploy') {
  description('Deploy service repos (stage / prod branches)')
}

folder('Airepro/release') {
  description('Stage → test → production promotion')
}

folder('Airepro/orchestrators') {
  description('Multi-service orchestrators')
}

// Seed job itself
pipelineJob('Airepro/seed-jobs') {
  description('Re-generate jobs from jenkins repo Job DSL. Re-run after services.yaml changes.')
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url('https://github.com/eliteprofessional/jenkins.git')
            credentials('eliteprofessional')
          }
          branch('main')
        }
      }
      scriptPath('seed/seedJob.groovy')
    }
  }
}

// Test jobs — one per airepro_cicd area
testAreas.each { area ->
  pipelineJob("Airepro/test/${area.id}") {
    description("Run ${area.label} Playwright tests from airepro_cicd (default: stage / server.airepro.in)")
    parameters {
      choiceParam('TEST_AREA', [area.id], 'Test area (fixed for this job)')
      choiceParam('ENV', ['stage', 'production'], 'Test environment')
      stringParam('GIT_BRANCH', 'fixes_ai', 'airepro_cicd branch')
      stringParam('API_BASE_URL', area.id == 'hire' ? 'https://server.airepro.in/api/v1' : '', 'BASE_URL override')
    }
    definition {
      cpsScm {
        scm {
          git {
            remote {
              url('https://github.com/eliteprofessional/jenkins.git')
              credentials('eliteprofessional')
            }
            branch('main')
          }
        }
        scriptPath('pipelines/Jenkinsfile.test-only')
      }
    }
  }
}

// Hire deploy jobs
pipelineJob('Airepro/deploy/hire-stage') {
  description('Deploy airepro-stage @ branch stage → stage.airepro.in / server.airepro.in')
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url('https://github.com/eliteprofessional/jenkins.git')
            credentials('eliteprofessional')
          }
          branch('main')
        }
      }
      scriptPath('pipelines/Jenkinsfile.deploy-stage')
    }
  }
}

pipelineJob('Airepro/deploy/hire-prod') {
  description('Deploy airepro-stage @ branch prod → production (manual — use release/hire-promote normally)')
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url('https://github.com/eliteprofessional/jenkins.git')
            credentials('eliteprofessional')
          }
          branch('main')
        }
      }
      scriptPath('pipelines/Jenkinsfile.deploy-prod')
    }
  }
}

// Primary release pipeline
pipelineJob('Airepro/release/hire-promote') {
  description('Deploy stage → test on server.airepro.in → deploy prod (recommended release path)')
  parameters {
    booleanParam('SKIP_PRODUCTION', false, 'Stage + test only')
    booleanParam('SKIP_STAGE_GATE', false, 'Emergency prod deploy without tests')
    stringParam('TEST_GIT_BRANCH', 'fixes_ai', 'airepro_cicd branch')
    choiceParam('TEST_AREAS', ['hire', 'hire,security', 'all'], 'Areas to test before prod')
  }
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url('https://github.com/eliteprofessional/jenkins.git')
            credentials('eliteprofessional')
          }
          branch('main')
        }
      }
      scriptPath('pipelines/Jenkinsfile.promote')
    }
  }
}

pipelineJob('Airepro/orchestrators/nightly-regression') {
  description('Run all airepro_cicd test areas against stage')
  parameters {
    stringParam('GIT_BRANCH', 'fixes_ai', 'airepro_cicd branch')
    booleanParam('DEPLOY_STAGE_FIRST', false, 'Deploy airepro-stage before tests')
  }
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url('https://github.com/eliteprofessional/jenkins.git')
            credentials('eliteprofessional')
          }
          branch('main')
        }
      }
      scriptPath('pipelines/Jenkinsfile.nightly-regression')
    }
  }
}
