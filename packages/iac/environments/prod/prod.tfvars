environment = "prod"
# Standby replica in a second AZ: automatic failover if the primary AZ goes down.
multi_az = true
# Two tasks from the start, so one failing task or AZ does not take the API down.
desired_count = 2
min_capacity  = 2
max_capacity  = 4
# API docs off in prod: the UI, /openapi.yaml and /v3/api-docs answer 404.
swagger_enabled = false
