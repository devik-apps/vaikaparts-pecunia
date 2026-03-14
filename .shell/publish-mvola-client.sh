#!/usr/bin/env bash

# =============================================================================
# publish-mvola-client.sh
#
# Extracts the version from the MVola client ZIP filename, checks whether that
# version has already been published (via git tags), and publishes it to GitHub
# Packages if it has not.
#
# ZIP filename convention: mvola-client-vX.Y.Z.zip
# Git tag convention     : mvola-client-vX.Y.Z
#
# Required environment variables:
#   GITHUB_ACTOR       - GitHub username used for authentication
#   GITHUB_TOKEN       - Personal Access Token or GITHUB_TOKEN with
#                        write:packages scope
#   GITHUB_REPOSITORY  - Repository in the form "owner/repo"
#
# Exit codes:
#   0 - Published successfully, or version already published (skipped)
#   1 - Unrecoverable error
#
# Usage:
#   bash .shell/publish-mvola-client.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly PROJECT_ROOT

readonly EXTRACT_DIR="${PROJECT_ROOT}/build/mvola-client-extracted"
readonly MAVEN_SETTINGS_FILE="${PROJECT_ROOT}/build/.maven-settings.xml"
readonly VERSION_OUTPUT_FILE="${PROJECT_ROOT}/build/published-mvola-client-version.txt"
readonly SKIP_FLAG_FILE="${PROJECT_ROOT}/build/mvola-client-publish-skipped"

readonly REQUIRED_ENV_VARS=("GITHUB_ACTOR" "GITHUB_TOKEN" "GITHUB_REPOSITORY")
readonly CLIENT_TAG_PREFIX="mvola-client-v"
readonly VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+$"

PUBLISH_GROUP_ID=""
readonly PUBLISH_ARTIFACT_ID="mvola-client"

log_info() { echo "[INFO]  $*" >&2; }
log_warn() { echo "[WARN]  $*" >&2; }
log_error() { echo "[ERROR] $*" >&2; }

validate_environment() {
	log_info "Validating environment..."

	for var in "${REQUIRED_ENV_VARS[@]}"; do
		if [[ -z "${!var:-}" ]]; then
			log_error "Required environment variable '${var}' is not set."
			exit 1
		fi
	done

	if ! command -v mvn >/dev/null 2>&1; then
		log_error "Maven (mvn) is not installed or not on PATH."
		exit 1
	fi

	if ! command -v unzip >/dev/null 2>&1; then
		log_error "'unzip' is not installed or not on PATH."
		exit 1
	fi

	log_info "Environment validation passed."
}

validate_token_format() {
	local token="${GITHUB_TOKEN}"

	if [[ ${#token} -lt 20 ]]; then
		log_error "GITHUB_TOKEN appears to be invalid (too short)."
		exit 1
	fi

	if [[ ! "${token}" =~ ^(ghp_|ghs_|github_pat_|gho_) ]]; then
		log_warn "GITHUB_TOKEN does not match a known GitHub token prefix."
		log_warn "Proceeding, but authentication may fail."
	fi
}

resolve_client_zip() {
	local matches=()

	while IFS= read -r -d '' file; do
		matches+=("${file}")
	done < <(find "${PROJECT_ROOT}" -maxdepth 1 -name "mvola-client-v*.zip" -print0 2>/dev/null)

	if [[ ${#matches[@]} -eq 0 ]]; then
		log_error "No file matching 'mvola-client-vX.Y.Z.zip' found at project root '${PROJECT_ROOT}'."
		log_error "Ensure the ZIP file follows the naming convention: mvola-client-vX.Y.Z.zip"
		exit 1
	fi

	if [[ ${#matches[@]} -gt 1 ]]; then
		log_error "Multiple MVola client ZIP files found at project root. Only one is allowed."
		for f in "${matches[@]}"; do
			log_error "  Found: ${f}"
		done
		exit 1
	fi

	echo "${matches[0]}"
}

extract_version_from_filename() {
	local zip_path="${1}"
	local filename
	filename="$(basename "${zip_path}")"

	# Expected format: mvola-client-vX.Y.Z.zip
	local version
	version="${filename#mvola-client-v}"
	version="${version%.zip}"

	if [[ ! "${version}" =~ ${VERSION_REGEX} ]]; then
		log_error "Cannot extract a valid semantic version from filename '${filename}'."
		log_error "Expected format: mvola-client-vX.Y.Z.zip (e.g. mvola-client-v1.2.0.zip)"
		exit 1
	fi

	echo "${version}"
}

is_version_already_published() {
	local version="${1}"
	local tag="${CLIENT_TAG_PREFIX}${version}"

	if git -C "${PROJECT_ROOT}" tag --list "${tag}" 2>/dev/null | grep -qx "${tag}"; then
		return 0 # already published
	fi

	return 1 # not yet published
}

extract_client_zip() {
	local zip_path="${1}"

	log_info "Extracting '${zip_path}'..."

	if [[ -d "${EXTRACT_DIR}" ]]; then
		log_info "Removing previous extraction directory..."
		rm -rf "${EXTRACT_DIR}"
	fi

	mkdir -p "${EXTRACT_DIR}"
	unzip -q "${zip_path}" -d "${EXTRACT_DIR}"

	# If the ZIP wraps everything inside a single top-level directory, flatten it.
	local entries entry_count single_entry tmp_dir
	entries=$(find "${EXTRACT_DIR}" -mindepth 1 -maxdepth 1)
	entry_count=$(echo "${entries}" | grep -c . || true)

	if [[ "${entry_count}" -eq 1 ]]; then
		single_entry=$(echo "${entries}" | head -n 1)
		if [[ -d "${single_entry}" ]]; then
			log_info "ZIP contains a single root directory. Flattening structure..."
			tmp_dir="${EXTRACT_DIR}-tmp"
			mv "${single_entry}" "${tmp_dir}"
			rm -rf "${EXTRACT_DIR}"
			mv "${tmp_dir}" "${EXTRACT_DIR}"
		fi
	fi

	if [[ ! -f "${EXTRACT_DIR}/pom.xml" ]]; then
		log_error "No pom.xml found in extracted client at '${EXTRACT_DIR}'."
		log_error "Verify that the ZIP is a valid Maven project."
		exit 1
	fi

	log_info "Extraction complete."
}

rewrite_pom_coordinates() {
	local version="${1}"

	local owner
	owner="${GITHUB_REPOSITORY%%/*}"

	PUBLISH_GROUP_ID="com.${owner}"

	log_info "Rewriting pom.xml coordinates..."
	log_info "  groupId    : ${PUBLISH_GROUP_ID}"
	log_info "  artifactId : ${PUBLISH_ARTIFACT_ID}"
	log_info "  version    : ${version}"

	local pom_file="${EXTRACT_DIR}/pom.xml"

	# Rewrite <groupId> — replace only the first occurrence (project-level, not dependency-level)
	local original_group
	original_group=$(mvn help:evaluate \
		-Dexpression=project.groupId \
		-q -DforceStdout \
		--file "${pom_file}" 2>/dev/null)

	local original_artifact
	original_artifact=$(mvn help:evaluate \
		-Dexpression=project.artifactId \
		-q -DforceStdout \
		--file "${pom_file}" 2>/dev/null)

	log_info "  Original groupId    : ${original_group}"
	log_info "  Original artifactId : ${original_artifact}"

	# Replace the project-level groupId (first occurrence only)
	sed -i "0,/<groupId>${original_group}<\/groupId>/s|<groupId>${original_group}</groupId>|<groupId>${PUBLISH_GROUP_ID}</groupId>|" "${pom_file}"

	# Replace the project-level artifactId (first occurrence only)
	sed -i "0,/<artifactId>${original_artifact}<\/artifactId>/s|<artifactId>${original_artifact}</artifactId>|<artifactId>${PUBLISH_ARTIFACT_ID}</artifactId>|" "${pom_file}"

	log_info "pom.xml coordinates rewritten successfully."
}

generate_maven_settings() {
	log_info "Generating temporary Maven settings..."

	local settings_dir
	settings_dir="$(dirname "${MAVEN_SETTINGS_FILE}")"
	mkdir -p "${settings_dir}"

	cat >"${MAVEN_SETTINGS_FILE}" <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_ACTOR}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

	chmod 600 "${MAVEN_SETTINGS_FILE}"
	log_info "Maven settings written."
}

cleanup() {
	if [[ -f "${MAVEN_SETTINGS_FILE}" ]]; then
		rm -f "${MAVEN_SETTINGS_FILE}"
		log_info "Temporary Maven settings removed."
	fi

	if [[ -d "${EXTRACT_DIR}" ]]; then
		rm -rf "${EXTRACT_DIR}"
		log_info "Extraction directory removed."
	fi
}

build_client() {
	log_info "Building MVola client JAR..."

	local output exit_code=0

	output=$(mvn clean package \
		--file "${EXTRACT_DIR}/pom.xml" \
		--batch-mode \
		-DskipTests 2>&1) || exit_code=$?

	if [[ ${exit_code} -ne 0 ]]; then
		log_error "Maven build failed with exit code ${exit_code}."
		echo "${output}" >&2
		exit "${exit_code}"
	fi

	log_info "Build successful."
}

set_pom_version() {
	local version="${1}"

	log_info "Setting pom.xml version to '${version}'..."

	mvn versions:set \
		-DnewVersion="${version}" \
		--file "${EXTRACT_DIR}/pom.xml" \
		--batch-mode \
		--quiet

	mvn versions:commit \
		--file "${EXTRACT_DIR}/pom.xml" \
		--batch-mode \
		--quiet

	log_info "pom.xml version set to '${version}'."
}

write_version_output() {
	local version="${1}"
	local output_dir
	output_dir="$(dirname "${VERSION_OUTPUT_FILE}")"

	mkdir -p "${output_dir}"
	echo "${version}" >"${VERSION_OUTPUT_FILE}"
	log_info "Published version written to '${VERSION_OUTPUT_FILE}'."
}

write_skip_flag() {
	local version="${1}"
	local output_dir
	output_dir="$(dirname "${SKIP_FLAG_FILE}")"

	mkdir -p "${output_dir}"
	echo "${version}" >"${SKIP_FLAG_FILE}"
	log_info "Skip flag written to '${SKIP_FLAG_FILE}'."
}

publish() {
	local version="${1}"
	local repository_url="https://maven.pkg.github.com/${GITHUB_REPOSITORY}"

	log_info "Publishing MVola client to GitHub Packages..."
	log_info "Repository : ${repository_url}"
	log_info "Actor      : ${GITHUB_ACTOR}"
	log_info "Version    : ${version}"

	local output exit_code=0

	output=$(mvn deploy \
		--file "${EXTRACT_DIR}/pom.xml" \
		--settings "${MAVEN_SETTINGS_FILE}" \
		--batch-mode \
		-DskipTests \
		-DaltDeploymentRepository="github::${repository_url}" 2>&1) || exit_code=$?

	if [[ ${exit_code} -ne 0 ]]; then
		if echo "${output}" | grep -q "status code: 409"; then
			log_error "Artifact version '${version}' already exists in GitHub Packages (409 Conflict)."
			log_error "The git tag check should have caught this. Verify tag '${CLIENT_TAG_PREFIX}${version}'."
			exit 1
		fi
		if echo "${output}" | grep -q "status code: 422"; then
			log_error "GitHub Packages rejected the artifact (422 Unprocessable Entity)."
			log_error "This usually means the groupId '${PUBLISH_GROUP_ID}' is not owned by the publishing organisation."
			log_error "Verify that GITHUB_REPOSITORY is set correctly and that the token has write:packages scope."
			exit 1
		fi
		log_error "Publish failed with exit code ${exit_code}."
		echo "${output}" >&2
		exit "${exit_code}"
	fi

	log_info "MVola client version '${version}' published successfully."
	write_version_output "${version}"
}

main() {
	trap cleanup EXIT

	validate_environment
	validate_token_format

	# Version is derived exclusively from the ZIP filename — single source of truth
	local client_zip version
	client_zip=$(resolve_client_zip)
	version=$(extract_version_from_filename "${client_zip}")

	log_info "ZIP file : ${client_zip}"
	log_info "Version  : ${version}"

	# Skip if this exact version is already published
	if is_version_already_published "${version}"; then
		log_info "Version '${version}' is already published (tag '${CLIENT_TAG_PREFIX}${version}' exists)."
		log_info "Skipping publication. Workflow exits with success."
		write_skip_flag "${version}"
		exit 0
	fi

	log_info "Version '${version}' has not been published yet. Proceeding..."

	extract_client_zip "${client_zip}"
	rewrite_pom_coordinates "${version}"
	build_client
	generate_maven_settings
	set_pom_version "${version}"
	publish "${version}"
}

main "$@"
