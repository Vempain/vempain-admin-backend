# Vempain Admin service, VAS

## REST API

[Local](http://localhost:8081/actuator/swagger-ui/index.html)

## Running in Docker

### Building the docker image

```shell
docker build -t vempain_admin_backend
```

### Running the docker container

```shell
docker run -dp 8080:8080 -e "SPRING_PROFILES_ACTIVE=local" vempain_admin
```

## Setting up development environment

### Vempain user (for site SSH)

The service uses SSH to transfer files to the remote web site. For testing one can do the same in localhost but a different user account.

There is a script at the root of the project called `testSetup.sh`. This will set up everything so that the test cases can be executed successfully.
It uses the [application.properties](service/src/test/resources/application.properties) file to set up the test environment. Please read through the script
to fully understand what it does. The script needs to be run as root, and it takes a single argument which your account on the machine. So, if you log on
to your machine as elvis, then you would execute the following command:

```shell
sudo ./testSetup.sh elvis
```

To clean up the setup, you can use the `testCleanup.sh` script which requires root privileges, so you execute it as follows:

```shell
sudo ./testCleanup.sh
```

### Required directories

You need to create both the local (i.e. Vempain Admin) converted file storage and remote (i.e. Vempain Simplex) web storage. As root run these commands:

```shell
mkdir -p /var/lib/vempain
mkdir -p /var/www/sites

chown <your login>: /var/lib/vempain
chown vempain: /var/www/sites
```

## Running locally

## Add vempain admin and database user to MariaDB

We use the same database for both admin as well as site.

```sql
CREATE USER 'vempain_admin'@'localhost' IDENTIFIED BY 'ADMIN_PASSWORD';
CREATE USER 'vempain_site'@'localhost' IDENTIFIED BY 'SITE_PASSWORD';

CREATE DATABASE vempain_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE vempain_site DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

GRANT Alter ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Create ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Create view ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Delete ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Drop ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Index ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Insert ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT References ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Select ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Trigger ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Show view ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Alter routine ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Create routine ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Create temporary tables ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Execute ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Lock tables ON vempain_admin.* TO 'vempain_admin'@'%';
GRANT Update ON vempain_admin.* TO 'vempain_admin'@'%';

GRANT Alter ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create view ON vempain_site.* TO 'vempain_site'@'%';
GRANT Delete ON vempain_site.* TO 'vempain_site'@'%';
GRANT Drop ON vempain_site.* TO 'vempain_site'@'%';
GRANT Index ON vempain_site.* TO 'vempain_site'@'%';
GRANT Insert ON vempain_site.* TO 'vempain_site'@'%';
GRANT References ON vempain_site.* TO 'vempain_site'@'%';
GRANT Select ON vempain_site.* TO 'vempain_site'@'%';
GRANT Trigger ON vempain_site.* TO 'vempain_site'@'%';
GRANT Show view ON vempain_site.* TO 'vempain_site'@'%';
GRANT Alter routine ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create routine ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create temporary tables ON vempain_site.* TO 'vempain_site'@'%';
GRANT Execute ON vempain_site.* TO 'vempain_site'@'%';
GRANT Lock tables ON vempain_site.* TO 'vempain_site'@'%';
GRANT Update ON vempain_site.* TO 'vempain_site'@'%';

FLUSH PRIVILEGES;
```

### Create admin user for UI

The scripts create a default admin user which is disabled. To enable the user, you need to set the password for the user. You can generate the password with the following command:

```shell
htpasswd -bnBC 12 "" testPassword  | tr -d ':'
```

Take the output which is the encoded password and run the following SQL:

```sql
UPDATE user_account SET password='<encoded password>' WHERE id=1;
```

## Running production

### Preparations

Vempain admin uses SSH to connect to the site and to transfer the files to the web site root. For this purpose you need to set up the SSH keys for the vempain
user. The following steps are required:

#### Set up local SSH keys

We need private and public SSH keys so that VAS can connect to the remote site server. You can place the keys in a secure location in the file system and then
point to the directory with the `ENV_VEMPAIN_ADMIN_SSH_CONFIG_DIR` environment variable. The keys should be named `id_ed25519` and `id_ed25519.pub`.
You need to also make sure that the files are only readable by the non-existing user ID 6666.

Choose a secure location for the keys, e.g. `/etc/vempain_admin/.ssh` on the host machine and run the following commands as root:

```shell
mkdir -p /etc/vempain_admin/.ssh
cd /etc/vempain_admin/.ssh
ssh-keygen -t ed25519 -f id_ed25519 -N "" -a 100
chown -R 6666:6666 /etc/vempain_admin/.ssh
```

This directory will be mounted into the container as defined by `ENV_VEMPAIN_ADMIN_SSH_CONFIG_DIR` to `/vempain_admin/vempain/.ssh`.

#### Set up remote SSH account

We also need an user account on the remote server which is used to transfer the files to the web site root. The user should have read/write access to the
website root. The user should also have a passwordless SSH access to the remote server. The following steps are required:

1. Create a user on the remote server, e.g. `vempain` and set up the SSH keys for the user. The following commands should be run on the remote server:
   ```shell
   adduser vempain
   mkdir ~vempain/.ssh
   echo "<local SSH public key>" > ~vempain/.ssh/authorized_keys
   chown -R vempain: ~vempain/.ssh
   chmod -R 0700 ~vempain/.ssh
   ```
2. The user should have read/write access to the website root. The following commands should be run on the remote server:
   ```shell
   chown -R vempain: /var/www/site
   ```
3. Test the connection from the docker host machine to the remote server. The following command should be run on the docker host machine:
   ```shell
   ssh -i /etc/vempain_admin/.ssh/id_ed25519 vempain@remote.tld
   ```

#### Set up remote database

Create the database and the related user on the remote server. The following commands should be run on the remote server:

```shell
CREATE USER 'vempain_site'@'localhost' IDENTIFIED BY 'password';
CREATE DATABASE `vempain_site` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

GRANT Alter ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create view ON vempain_site.* TO 'vempain_site'@'%';
GRANT Delete ON vempain_site.* TO 'vempain_site'@'%';
GRANT Drop ON vempain_site.* TO 'vempain_site'@'%';
GRANT Index ON vempain_site.* TO 'vempain_site'@'%';
GRANT Insert ON vempain_site.* TO 'vempain_site'@'%';
GRANT References ON vempain_site.* TO 'vempain_site'@'%';
GRANT Select ON vempain_site.* TO 'vempain_site'@'%';
GRANT Trigger ON vempain_site.* TO 'vempain_site'@'%';
GRANT Show view ON vempain_site.* TO 'vempain_site'@'%';
GRANT Alter routine ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create routine ON vempain_site.* TO 'vempain_site'@'%';
GRANT Create temporary tables ON vempain_site.* TO 'vempain_site'@'%';
GRANT Execute ON vempain_site.* TO 'vempain_site'@'%';
GRANT Lock tables ON vempain_site.* TO 'vempain_site'@'%';
GRANT Update ON vempain_site.* TO 'vempain_site'@'%';

FLUSH PRIVILEGES;
```

### Starting up the service and database

For production the service as well as the admin database is running in a docker container. For this purpose there's a `docker-compose.yaml` file in the root
directory. First you should create a separate directory, e.g. prod to which you copy the `docker-compose.yaml`. The script takes several environment variables,
prefixed with ENV_, which you can set from the command line. The following variables should be set:

| Variable                           | Description                                                                                              | Example         |
|------------------------------------|----------------------------------------------------------------------------------------------------------|-----------------|
| ENV_VEMPAIN_TYPE                   | The type of the vempain service, can be e.g. `prod` or `stg`. Prefix to container name                   | prod            |
| ENV_VEMPAIN_ADMIN_VERSION          | The build version of the vempain-admin docker image, should be in form of major.minor.patch.             | 1.2.3           |
| ENV_VEMPAIN_ADMIN_HOSTNAME         | The hostname part of the vempain admin service URL.                                                      | va.domain.tld   |
| ENV_VEMPAIN_ADMIN_SSH_CONFIG_DIR   | The directory where the ssh configuration is stored. This is used to copy the ssh keys to the container. | /home/user/.ssh |
| ENV_VEMPAIN_ADMIN_CONVERTED_DIR    | Main directory of all converted files                                                                    | /converted      |
| ENV_VEMPAIN_ADMIN_DB_ROOT_PASSWORD | The root password of the vempain admin database.                                                         | password        |
| ENV_VEMPAIN_ADMIN_DB_NAME          | The name of the vempain admin database.                                                                  | vempain_admin   |
| ENV_VEMPAIN_ADMIN_DB_USER          | The user name of the vempain admin database.                                                             | vempain         |
| ENV_VEMPAIN_ADMIN_DB_PASSWORD      | The user password of the vempain admin database.                                                         | password        |
| ENV_VEMPAIN_SITE_DB_ADDRESS        | The address of the vempain site database.                                                                | remote.tld      |
| ENV_VEMPAIN_SITE_DB_USER           | The username of the vempain site database.                                                               | vempain_site    |
| ENV_VEMPAIN_SITE_DB_NAME           | The name of the vempain site database.                                                                   | vempain_site    |
| ENV_VEMPAIN_SITE_DB_PASSWORD       | The password of the vempain site database.                                                               | password        |
| ENV_VEMPAIN_SITE_SSH_ADDRESS       | The address of the vempain site.                                                                         | remote.tld      |
| ENV_VEMPAIN_SITE_SSH_PORT          | The port of the vempain site.                                                                            | 22              |
| ENV_VEMPAIN_SITE_SSH_USER          | The user to connect to the vempain site.                                                                 | vempain         |
| ENV_VEMPAIN_SITE_WWW_ROOT          | Remote path on the site where the web root is.                                                           | /var/www/site   |

Using all these variables, the docker compose can be started with the following command: 

```shell
ENV_VEMPAIN_ADMIN_VERSION=1.2.3 \
ENV_VEMPAIN_ADMIN_DB_ROOT_PASSWORD=password \
ENV_VEMPAIN_ADMIN_DB_NAME=vempain_admin \
ENV_VEMPAIN_ADMIN_DB_USER=vempain_admin \
ENV_VEMPAIN_ADMIN_DB_PASSWORD=password \
ENV_VEMPAIN_ADMIN_HOSTNAME=va.domain.tld \
ENV_VEMPAIN_ADMIN_SSH_CONFIG_DIR=/home/user/.ssh \
ENV_VEMPAIN_ADMIN_CONVERTED_DIR=/converted \
ENV_VEMPAIN_SITE_DB_ADDRESS=remote.tld \
ENV_VEMPAIN_SITE_DB_NAME=vempain_site \
ENV_VEMPAIN_SITE_DB_USER=vempain_site \
ENV_VEMPAIN_SITE_DB_PASSWORD=password \
ENV_VEMPAIN_SITE_SSH_ADDRESS=remote.tld \
ENV_VEMPAIN_SITE_SSH_PORT=22 \
ENV_VEMPAIN_SITE_SSH_USER=vempain \
ENV_VEMPAIN_SITE_WWW_ROOT=/var/www/site \
ENV_VEMPAIN_TYPE=prod \
docker compose up -d
```

### Security concerns

#### Site database

The site database should ideally be accessed through a VPN or a private network such as Wireguard. If this is not possible, the database should be secured
with a strong password and the database should be regularly backed up.
