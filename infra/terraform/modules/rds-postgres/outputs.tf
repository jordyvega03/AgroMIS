output "endpoint"       { value = aws_db_instance.main.endpoint }
output "port"           { value = aws_db_instance.main.port }
output "db_name"        { value = aws_db_instance.main.db_name }
output "username"       { value = aws_db_instance.main.username }
output "security_group" { value = aws_security_group.rds.id }
