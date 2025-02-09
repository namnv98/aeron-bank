docker compose down
docker rmi -f $(docker images -q)
#docker compose up