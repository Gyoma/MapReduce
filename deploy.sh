#!/bin/bash

mvn package

login="agolovin-22"
remoteFolder="/tmp/$login/"
masterProgram="master-program.jar"
serverProgram="server-program.jar"

# List of servers
serverComputers=("tp-3a209-04:10335" "tp-3a209-05:10335")

# Master
masterComputer="tp-3a209-06"

# Path to a file to take data from
fileName="/cal/commoncrawl/CC-MAIN-20230320175948-20230320205948-00274.warc.wet"

# command0=("ssh-copy-id" "$login@$masterComputer")
# echo ${command0[*]}
# "${command0[@]}"

# Launch servers
for item in ${serverComputers[@]}; do
  #command0=("ssh-copy-id" "$login@$c")

  adr=${item%%:*}
  port=${item#*:}

  command1=("ssh" "$login@$adr" "lsof -ti tcp:$port | xargs kill -9")
  command2=("ssh" "$login@$adr" "rm -rf $remoteFolder;mkdir $remoteFolder")
  command3=("scp" "target/$serverProgram" "$login@$adr:$remoteFolder$serverProgram")
  command4=("ssh" "$login@$adr" "cd $remoteFolder;java -jar $serverProgram -p=\"$port\"")
  
  #echo ${command0[*]}
  #"${command0[@]}"
  echo ${command1[*]}
  "${command1[@]}"
  echo ${command2[*]}
  "${command2[@]}"
  echo ${command3[*]}
  "${command3[@]}"
  echo ${command4[*]}
  "${command4[@]}" &
done

# The master launch commands
# command0=("ssh" "$login@$masterComputer" "lsof -ti tcp:$masterPort | xargs kill -9")
command1=("ssh" "$login@$masterComputer" "rm -rf $remoteFolder;mkdir $remoteFolder")
command2=("scp" "target/$masterProgram" "$login@$masterComputer:$remoteFolder$masterProgram")
#command3=("scp" "$fileName" "$login@$masterComputer:$remoteFolder$fileName")
addresses=$(printf ";%s" "${serverComputers[@]}")
addresses=${addresses:1}
args="-path=\"$fileName\" -s=\"$addresses\""
command4=("ssh" "$login@$masterComputer" "cd $remoteFolder;java -jar $masterProgram $args")

#echo ${command0[*]}
#"${command0[@]}"
echo ${command1[*]}
"${command1[@]}"
echo ${command2[*]}
"${command2[@]}"
#echo ${command3[*]}
#"${command3[@]}"
echo ${command4[*]}
"${command4[@]}"
