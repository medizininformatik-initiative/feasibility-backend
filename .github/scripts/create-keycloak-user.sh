#!/bin/bash -e

docker exec -u0 integration-test-auth-1 /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user keycloakadmin --password keycloak
docker exec -u0 integration-test-auth-1 /opt/keycloak/bin/kcadm.sh create users -s username=testuser -s email=test@example.com -s enabled=true -s emailVerified=true -r feasibility
docker exec -u0 integration-test-auth-1 /opt/keycloak/bin/kcadm.sh add-roles --uusername testuser --rolename FeasibilityUser -r feasibility
docker exec -u0 integration-test-auth-1 /opt/keycloak/bin/kcadm.sh set-password -r feasibility --username testuser --new-password testpassword
