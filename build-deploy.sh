# Copy the files needed to a build directory and zip it up
# Send it to the production server

PROJECT=jerbil

# exit and report the failure if any command fails (from https://stackoverflow.com/questions/1378274/in-a-bash-script-how-can-i-exit-the-entire-script-if-a-certain-condition-occurs)
exit_trap () {
  local lc="$BASH_COMMAND" rc=$?
  echo "Command [$lc] exited with code [$rc]"
}
trap exit_trap EXIT
set -e

BD=$PROJECT-build

echo "Java"
mvn-all.sh

cd /home/winterwell/$PROJECT
mvn clean
mvn install -DskipTests
mvn package -DskipTests

# no js

# copy to www server
ln -s `ls target/*dependencies.jar` jerbil-all.jar
scp jerbil-all.jar aberdeen:~/winterwell-www/software

# ./build-deploy2.sh $PROJECT
