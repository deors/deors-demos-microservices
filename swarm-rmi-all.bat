call docker-env docker-swarm-manager-1
for /F %%f in ('docker ps -a -q') do (docker rm %%f)
for /F %%f in ('docker images -q') do (docker rmi --force %%f)

call docker-env docker-swarm-manager-2
for /F %%f in ('docker ps -a -q') do (docker rm %%f)
for /F %%f in ('docker images -q') do (docker rmi --force %%f)

call docker-env docker-swarm-manager-3
for /F %%f in ('docker ps -a -q') do (docker rm %%f)
for /F %%f in ('docker images -q') do (docker rmi --force %%f)

call docker-env docker-swarm-worker-1
for /F %%f in ('docker ps -a -q') do (docker rm %%f)
for /F %%f in ('docker images -q') do (docker rmi --force %%f)

call docker-env docker-swarm-worker-2
for /F %%f in ('docker ps -a -q') do (docker rm %%f)
for /F %%f in ('docker images -q') do (docker rmi --force %%f)

call docker-env docker-swarm-worker-3
for /F %%f in ('docker ps -a -q') do (docker rm %%f)
for /F %%f in ('docker images -q') do (docker rmi --force %%f)
