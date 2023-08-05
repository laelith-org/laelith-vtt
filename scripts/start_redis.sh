# use redis-stack-server for production https://hub.docker.com/r/redis/redis-stack-server
docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 -v /local-data/:/data redis/redis-stack:latest