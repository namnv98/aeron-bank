docker compose down
docker stop $(docker ps -q)
docker rmi -f $(docker images -q)
docker compose up