#!/usr/bin/env ash

echo "${SSH_PASSWORD}" > ./CONNECTION_PASSWORD_FILE
echo "${ASK_BECOME_PASSWORD}" > ./BECOME_PASSWORD_FILE
ansible-playbook --connection-password-file CONNECTION_PASSWORD_FILE --become-password-file BECOME_PASSWORD_FILE playbooks/${PLAYBOOK}