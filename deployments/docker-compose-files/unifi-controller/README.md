Note that the init script method will only work on first run. If you start the mongodb container without an init script it will generate test data automatically and you will have to manually create your databases, or restart with a clean /data/db volume and an init script mounted.

