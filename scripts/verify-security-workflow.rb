#!/usr/bin/env ruby
# frozen_string_literal: true

require 'yaml'

ROOT = File.expand_path('..', __dir__)
WORKFLOW_PATH = File.join(ROOT, '.github/workflows/security.yml')
ACTION_PATH = File.join(ROOT, '.github/actions/security-scan/action.yml')

def load_yaml(path)
  YAML.safe_load(File.read(path), aliases: true)
rescue Psych::SyntaxError => e
  abort("invalid YAML #{path}: #{e.message}")
end

def assert(condition, message)
  abort(message) unless condition
end

def external_uses(node, result = [])
  case node
  when Hash
    node.each do |key, value|
      result << value if key == 'uses' && value.is_a?(String) && !value.start_with?('./')
      external_uses(value, result)
    end
  when Array
    node.each { |value| external_uses(value, result) }
  end
  result
end

def values_for_key(node, searched_key, result = [])
  case node
  when Hash
    node.each do |key, value|
      result << value if key == searched_key
      values_for_key(value, searched_key, result)
    end
  when Array
    node.each { |value| values_for_key(value, searched_key, result) }
  end
  result
end

workflow = load_yaml(WORKFLOW_PATH)
action = load_yaml(ACTION_PATH)
triggers = workflow['on'] || workflow[true]
jobs = workflow.fetch('jobs')
pr_job = jobs.fetch('pull-request-contract')
current_job = jobs.fetch('privileged-current-line')
scheduled_job = jobs.fetch('scheduled-maintenance-lines')

assert(triggers.key?('pull_request'), 'pull_request trigger is required')
assert(triggers.key?('schedule'), 'weekly schedule trigger is required')
assert(workflow.dig('permissions', 'contents') == 'read', 'workflow default permissions must be contents: read')

pr_text = pr_job.to_s
assert(pr_job['if'].include?("github.event_name == 'pull_request'"), 'PR job must be explicitly PR-only')
assert(pr_job.dig('permissions', 'contents') == 'read', 'PR job must be read-only')
assert(!pr_text.include?('${{ secrets.'), 'PR job must not read secrets')
assert(!pr_text.include?('./.github/actions/'), 'PR job must not run repository-local actions')
assert(pr_text.include?('Privileged dependency scan deferred'), 'PR job must record the deferred privileged scan')

current_if = current_job.fetch('if')
assert(current_if.include?('github.ref_protected'), 'push scan must require a protected ref')
assert(current_if.include?('workflow_dispatch'), 'controlled workflow dispatch must enable the privileged scan')
assert(!current_if.include?('pull_request'), 'privileged current-line scan must not run for PR events')
assert(scheduled_job.fetch('if').include?('schedule'), 'scheduled matrix must be schedule-only')
assert(scheduled_job.fetch('if').include?('github.ref_protected'), 'schedule must require protected default branch')
assert(scheduled_job.to_s.include?('Require protected maintenance branch'),
       'schedule must verify each target maintenance branch is protected before secrets are used')

[current_job, scheduled_job].each do |job|
  assert(job.dig('permissions', 'contents') == 'read', 'privileged jobs need contents: read')
  assert(job.dig('permissions', 'security-events') == 'write', 'privileged jobs need security-events: write')
  assert(job.to_s.include?('secrets.MAVEN_SETTINGS_XML'), 'privileged jobs must receive MAVEN_SETTINGS_XML')
  assert(job.to_s.include?('secrets.NVD_API_KEY'), 'privileged jobs must receive NVD_API_KEY')
end

matrix = scheduled_job.dig('strategy', 'matrix', 'include')
assert(matrix.map { |entry| entry['branch'] }.sort == ['feature/3.3.x', 'feature/4.0.x'],
       'schedule must dispatch both maintenance branches')

action_text = action.to_s
root_pom = File.read(File.join(ROOT, 'pom.xml'))
required_fragments = [
  'dependency-check-maven:12.2.2:update-only',
  'dependency-check-maven:12.2.2:aggregate',
  '-Ddependency-check.skip=false',
  '-DautoUpdate=false',
  '-DfailOnError=true',
  '-DfailBuildOnCVSS=7',
  '-DskipTestScope=false',
  'target/sbom/runtime',
  '-DincludeTestScope=false',
  'target/sbom/build-test',
  '-DincludeTestScope=true',
  'github/codeql-action/upload-sarif@',
  'dependency-check-report.sarif',
  'actions/cache/restore@',
  'actions/cache/save@',
  'schema11',
  'dependency-check-cache-date.outputs.date'
]
required_fragments.each { |fragment| assert(action_text.include?(fragment), "security action is missing #{fragment}") }
assert(!action_text.include?('continue-on-error'), 'security action must not suppress failures')
if root_pom.include?('<revision>4.0.x.')
  assert(action_text.include?('ddd4j.maven.home'), '4.0.x security goals require the Maven 3 home bridge')
else
  assert(!action_text.include?('ddd4j.maven.home'), '3.3.x must retain its Maven 3 branch contract')
end
values_for_key([workflow, action], 'if').each do |condition|
  assert(condition != false && condition.to_s.strip != 'false', 'security workflow must not contain if: false')
end

local_scan_jobs = jobs.select { |_name, job| job.to_s.include?('./.github/actions/security-scan') }.keys.sort
assert(local_scan_jobs == ['privileged-current-line', 'scheduled-maintenance-lines'],
       'only privileged jobs may invoke the secret-bearing local security action')

external_uses([workflow, action]).each do |uses|
  revision = uses.split('@', 2)[1]
  assert(revision&.match?(/\A[0-9a-f]{40}\z/), "external action is not pinned to a full SHA: #{uses}")
end

puts 'security workflow verified: secret isolation, protected gate, SARIF, scoped SBOMs, trusted cache'
