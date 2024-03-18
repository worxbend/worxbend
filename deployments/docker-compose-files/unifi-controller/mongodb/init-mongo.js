const MONGODB_PASS='<replace_it>'
db.getSiblingDB("unifi").createUser({
    user: "unifi",
    pwd: `${MONGODB_PASS}`,
    roles: [{role: "dbOwner", db: "unifi"}]
});

db.getSiblingDB("unifi_stat").createUser({
    user: "unifi",
    pwd: `${MONGODB_PASS}`,
    roles: [{role: "dbOwner", db: "unifi_stat"}]
});

db.getSiblingDB("admin").createUser({
    user: "unifi",
    pwd: `${MONGODB_PASS}`,
    roles: [{role: "dbOwner", db: "admin"}]
});
