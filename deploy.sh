#!/bin/bash

mvn package

login="agolovin-22"
remoteFolder="/tmp/$login/"
masterProgram="master-program.jar"
serverProgram="server-program.jar"

computers=("tp-3a209-04:10333" "tp-3a209-05:10334")
masterComputer="tp-3a209-06:10341"

#fileName="test.txt"
fileName="/cal/commoncrawl/CC-MAIN-20230320175948-20230320205948-00274.warc.wet"

masterAdr=${masterComputer%%:*}
masterPort=${masterComputer#*:}

# command0=("ssh-copy-id" "$login@$masterAdr")
# echo ${command0[*]}
# "${command0[@]}"

for item in ${computers[@]}; do
  #command0=("ssh-copy-id" "$login@$c")

  adr=${item%%:*}
  port=${item#*:}

  command1=("ssh" "$login@$adr" "lsof -ti tcp:$port | xargs kill -9")
  command2=("ssh" "$login@$adr" "rm -rf $remoteFolder;mkdir $remoteFolder")
  command3=("scp" "target/$serverProgram" "$login@$adr:$remoteFolder$serverProgram")
  command4=("ssh" "$login@$adr" "cd $remoteFolder;java -jar $serverProgram -p=\"$port\" > suka$adr:$port.txt")
  
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

command0=("ssh" "$login@$masterAdr" "lsof -ti tcp:$masterPort | xargs kill -9")
command1=("ssh" "$login@$masterAdr" "rm -rf $remoteFolder;mkdir $remoteFolder")
command2=("scp" "target/$masterProgram" "$login@$masterAdr:$remoteFolder$masterProgram")
#command3=("scp" "$fileName" "$login@$masterAdr:$remoteFolder$fileName")
addresses=$(printf ";%s" "${computers[@]}")
addresses=${addresses:1}
args="-path=\"$fileName\" -s=\"$addresses\" -p=\"$masterPort\""
command4=("ssh" "$login@$masterAdr" "cd $remoteFolder;java -jar $masterProgram $args")

echo ${command0[*]}
"${command0[@]}"
echo ${command1[*]}
"${command1[@]}"
echo ${command2[*]}
"${command2[@]}"
#echo ${command3[*]}
#"${command3[@]}"
echo ${command4[*]}
"${command4[@]}"
