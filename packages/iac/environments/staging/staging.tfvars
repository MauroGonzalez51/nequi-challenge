# Same shape as dev: single-AZ RDS, 1 to 2 tasks, Swagger on.
environment     = "staging"
multi_az        = false
desired_count   = 1
min_capacity    = 1
max_capacity    = 2
swagger_enabled = true
