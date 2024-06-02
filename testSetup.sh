#!/usr/bin/env bash

# Parse the options
while [[ "$#" -gt 0 ]]; do
  case $1 in
    --developer-name) DEVELOPER_NAME="$2"; shift ;;
    --skip-root-check) SKIP_ROOT_CHECK="true" ;;
    --skip-answers) SKIP_ANSWERS="true" ;;
    --help) SHOW_HELP="true" ;;
    *) echo "Unknown parameter passed: $1"; exit 1 ;;
  esac
  shift
done

function printHelp() {
  echo "This script sets up the test environment for the project."
  echo "It uses the values from the service/src/test/resources/application.properties file to set up the environment."
  echo "This includes users and directories. Please review the property file before running this script to fully understand the changes."
  echo
  echo "Options:"
  echo "  --developer-name  The name of the developer for whom the test environment is being set up"
  echo "  --skip-root-check  Skip checking whether the root executes this script. Default is to perform the check"
  echo "  --skip-answers  Skip the confirmation prompts"
  echo "  --help  Show this help message"

}

# If the help parameter is passed, show the help message
if [ -n "${SHOW_HELP}" ]; then
  printHelp
  exit 0
fi

# Set the developer name to the current user if it is not set
if [ -z "${DEVELOPER_NAME}" ]; then
  echo "ERROR: Developer name must be set with --developer-name"
  echo
  printHelp
  exit 1
fi

# Make sure the username exists
if ! id "${DEVELOPER_NAME}" &>/dev/null; then
  echo "User ${DEVELOPER_NAME} does not exist"
  exit 1
fi

# Check that this script is being run as root, except if the SKIP_ROOT_CHECK variable is set
if [ -z "${SKIP_ROOT_CHECK}" ] && [ "$EUID" -ne 0 ]; then
  echo "Please run this script as root"
  exit 1
fi

# Get the uid of the given username

uid=$(id -u "${DEVELOPER_NAME}")

TEST_PROPERTY_FILE="service/src/test/resources/application.properties"

if [ -z "${SKIP_ROOT_CHECK}" ]; then
  echo "This script will setup the test environment for the project. It may delete the existing test directory and test file."
  echo "It uses the values from the ${TEST_PROPERTY_FILE} file to set up the environment. This"
  echo "includes users and directories. Please review the property file before running this script to fully understand the changes."
  echo
  echo "Do you want to continue? (y/n)"
  read answer

  if [ "$answer" == "${answer#[Yy]}" ] ;then
    echo "Test environment setup cancelled! Exiting..."
    exit 0
  fi
fi

# Read the property file
echo "Reading the property file ${TEST_PROPERTY_FILE}"
# Read the properties file and assign variables
while IFS='=' read -r key value; do
  # Skip empty lines and comments
  if [ -z "$key" ] || [[ "$key" == \#* ]]; then
    continue
  fi
  key=$(echo $key | tr '.' '_' | tr '-' '_')  # Replace '.' or '-' with '_' in the key (optional)
  eval "${key}=\"${value}\""
  echo "Set ${key}='${value}'"
done < "${TEST_PROPERTY_FILE}"

if [ -z "${SKIP_ROOT_CHECK}" ]; then
  # Make sure the user wants to run the script
  echo "These are the settings the script will use. Do you want to continue? (y/n)"
  read answer

  if [ "$answer" == "${answer#[Yy]}" ] ;then
    echo "Test environment setup cancelled! Exiting..."
    exit 0
  fi
fi

# Check if vempain_admin_ssh_user user exists, if not, create it
if id "${vempain_admin_ssh_user}" &>/dev/null; then
  echo "User ${vempain_admin_ssh_user} already exists"
else
  echo "Creating user ${vempain_admin_ssh_user}"
  useradd -m -o -u ${uid} -s /bin/bash -d "${vempain_admin_ssh_home_dir}" "${vempain_admin_ssh_user}"
fi


# Check if vempain_admin_file_converted_directory exists, is a directory
if [ -d "${vempain_admin_file_converted_directory}" ]; then
  # Check if it is owned by the vempain_admin_ssh_user
  if [ "$(stat -c %U "${vempain_admin_file_converted_directory}")" == "${vempain_admin_ssh_user}" ]; then
    echo "Directory ${vempain_admin_file_converted_directory} already exists and is owned by ${vempain_admin_ssh_user}"
  else
    echo "Directory ${vempain_admin_file_converted_directory} already exists but is not owned by ${vempain_admin_ssh_user}"
    echo "Please delete the directory and run the script again"
    exit 1
  fi

  echo "Deleting the existing directory ${vempain_admin_file_converted_directory}"
  rm -rf "${vempain_admin_file_converted_directory}"
fi

mkdir -p "${vempain_admin_file_converted_directory}"
chown -R "${vempain_admin_ssh_user}:${vempain_admin_ssh_user}" "${vempain_admin_file_converted_directory}"
chmod 0777 "${vempain_admin_file_converted_directory}"

# Check if vempain_admin_ssh_home_dir directory exists and is a directory
if [ -d "${vempain_admin_ssh_home_dir}" ]; then
  echo "Deleting the existing directory ${vempain_admin_ssh_home_dir}"
  rm -rf "${vempain_admin_ssh_home_dir}"
fi

mkdir -p "${vempain_admin_ssh_home_dir}"

# Make sure the .ssh directory exists for the vempain_admin_ssh_user
if [ ! -d "${vempain_admin_ssh_home_dir}/.ssh" ]; then
  echo ".ssh directory does not exist for ${vempain_admin_ssh_user}, creating it"
  mkdir -p "${vempain_admin_ssh_home_dir}/.ssh"
fi

# Generate the ed25519 key pair for the vempain_admin_ssh_user
if [ -f "${vempain_admin_ssh_home_dir}/.ssh/id_ed25519" ]; then
  echo "Deleting the existing key pair for ${vempain_admin_ssh_user}"
  rm -f "${vempain_admin_ssh_home_dir}/.ssh/id_ed25519"
fi

echo "Generating the ed25519 key pair for ${vempain_admin_ssh_user}"
ssh-keygen -t ed25519 -f "${vempain_admin_ssh_home_dir}/.ssh/id_ed25519" -N ""

chown -R "${vempain_admin_ssh_user}:${vempain_admin_ssh_user}" "${vempain_admin_ssh_home_dir}"
chmod -R 0777 "${vempain_admin_ssh_home_dir}/.ssh/"
chmod 0777 "${vempain_admin_ssh_home_dir}/.ssh"

# Check if vempain_site_www_root directory exists and is a directory
if [ -d "${vempain_site_www_root}" ]; then
  echo "Deleting the existing directory ${vempain_site_www_root}"
  rm -rf "${vempain_site_www_root}"
fi

# Create vempain_site_www_root directory
mkdir -p "${vempain_site_www_root}"
chmod 0777 "${vempain_site_www_root}"

# Check if vempain_site_ssh_user user exists, if not, create it
if id "${vempain_site_ssh_user}" &>/dev/null; then
  echo "User ${vempain_site_ssh_user} already exists"
else
  echo "Creating user ${vempain_site_ssh_user}"
  useradd -m -o -u ${uid} -s /bin/bash -d "${vempain_site_ssh_home_dir}" "${vempain_site_ssh_user}"
fi

# If the .ssh directory does not exist for the vempain_site_ssh_user, create it
if [ ! -d "${vempain_site_ssh_home_dir}/.ssh" ]; then
  echo "${vempain_site_ssh_home_dir}/.ssh directory does not exist for ${vempain_site_ssh_user}, creating it"
  mkdir -p "${vempain_site_ssh_home_dir}/.ssh"
fi

# Copy the admin user's public key to the vempain_site_ssh_user's authorized_keys file
echo "Copying the public key for ${vempain_admin_ssh_user} to the authorized_keys file for ${vempain_site_ssh_user}"
cat "${vempain_admin_ssh_home_dir}/.ssh/id_ed25519.pub" >> "${vempain_site_ssh_home_dir}/.ssh/authorized_keys"

chown -R "${vempain_site_ssh_user}:${vempain_site_ssh_user}" "${vempain_site_www_root}"
chown -R "${vempain_site_ssh_user}:${vempain_site_ssh_user}" "${vempain_site_ssh_home_dir}"
chmod 0700 "${vempain_site_ssh_home_dir}/.ssh/"
chmod 0700 "${vempain_site_ssh_home_dir}/.ssh/authorized_keys"

echo "Test environment setup completed successfully!"
