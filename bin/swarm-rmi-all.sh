eval $(docker-machine env docker-swarm-manager-1)
docker rm $(docker ps -a -q)
docker rmi --force $(docker images -q)

eval $(docker-machine env docker-swarm-manager-3)
docker rm $(docker ps -a -q)
docker rmi --force $(docker images -q)

eval $(docker-machine env docker-swarm-manager-3)
docker rm $(docker ps -a -q)
docker rmi --force $(docker images -q)

eval $(docker-machine env docker-swarm-worker-1)
docker rm $(docker ps -a -q)
docker rmi --force $(docker images -q)

eval $(docker-machine env docker-swarm-worker-2)
docker rm $(docker ps -a -q)
docker rmi --force $(docker images -q)

eval $(docker-machine env docker-swarm-worker-3)
docker rm $(docker ps -a -q)
docker rmi --force $(docker images -q)
