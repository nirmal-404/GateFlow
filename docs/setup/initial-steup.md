docker exec gateflow-postgres pg_isready -U gateflow -d gateflow

docker exec gateflow-redis redis-cli ping

docker exec -it gateflow-postgres psql -U gateflow -d gateflow
\dt
\q