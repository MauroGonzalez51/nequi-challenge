environment = "dev"
# Single-AZ RDS: cheaper, and dev data is disposable.
multi_az = false
# Tasks at the start. Autoscaling moves it between min and max (Terraform ignores later changes).
desired_count = 1
min_capacity  = 1
max_capacity  = 2
# API docs on, to try the endpoints from the browser.
swagger_enabled = true
