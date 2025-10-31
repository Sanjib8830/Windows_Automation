pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  parameters {
    choice(
      name: 'PLAYBOOK',
      choices: [
        'maintainence.yml',
        'clear_chrome_cache.yml',
        'disk_defragmentation.yml',
        'local_user_management.yml',
        'office_laptop_software_setup.yml',
        'osupgrade.yml',
        'thunderbird_archival.yml',
        'windows_drift.yml',
        'windows_health_check.yml'
      ],
      description: 'Select which Ansible playbook to run'
    )

    string(
      name: 'TARGET_HOSTNAME',
      defaultValue: 'ICFADHLAP06586',
      description: 'Enter the short hostname (without domain, e.g. ICFADHLAP06586)'
    )

    booleanParam(
      name: 'DRY_RUN',
      defaultValue: false,
      description: 'Run in check mode (--check --diff)'
    )

    string(
      name: 'EXTRA_VARS',
      defaultValue: '',
      description: 'Optional extra vars (e.g. version=1.2.3 flag=true)'
    )
  }

  stages {
    stage('Execute Playbook') {
      steps {
        script {
          // Base directory paths
          def baseDir = "/var/lib/jenkins/workspace/playbooks"
          def inventory = "${baseDir}/inventory.ini"
          def playbookPath = "${baseDir}/${params.PLAYBOOK}"

          // Construct FQDN automatically
          def fqdn = "${params.TARGET_HOSTNAME}.IndoStar.com"
          echo "Resolving ${fqdn}..."

          // Resolve FQDN to IP using nslookup
          def ip = sh(
            script: "nslookup ${fqdn} | awk '/Address: / {print \$2}' | tail -n1",
            returnStdout: true
          ).trim()

          if (!ip) {
            error "❌ Could not resolve ${fqdn} to an IP address. Please check DNS or the hostname."
          }

          echo "✅ Resolved ${fqdn} to ${ip}"

          // Build the Ansible command
          def cmd = "ansible-playbook -i ${inventory} ${playbookPath} --limit ${ip}"

          if (params.DRY_RUN) {
            cmd += " --check --diff"
          }

          if (params.EXTRA_VARS?.trim()) {
            cmd += " -e \"${params.EXTRA_VARS.trim()}\""
          }

          echo "Running command: ${cmd}"
          sh "${cmd}"
        }
      }
    }
  }

  post {
    always {
      echo "✅ Job completed. Check console output for Ansible playbook results."
    }
  }
}
