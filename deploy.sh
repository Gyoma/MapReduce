#!/bin/bash

mvn package

login="agolovin-22"
remoteFolder="/tmp/$login/"
masterProgram="master-program.jar"
serverProgram="server-program.jar"

port="10333"
computers=("tp-1a226-03" "tp-1a226-04")
masterComputer="tp-1a226-05"

testFileName="test.txt"

# command0=("ssh-copy-id" "$login@$masterComputer")
# echo ${command0[*]}
# "${command0[@]}"

for c in ${computers[@]}; do
  #command0=("ssh-copy-id" "$login@$c")
  command1=("ssh" "$login@$c" "lsof -ti tcp:$port | xargs kill -9")
  command2=("ssh" "$login@$c" "rm -rf $remoteFolder;mkdir $remoteFolder")
  command3=("scp" "target/$serverProgram" "$login@$c:$remoteFolder$serverProgram")
  command4=("ssh" "$login@$c" "cd $remoteFolder;java -jar $serverProgram -p=\"$port\"")
  
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


command1=("ssh" "$login@$masterComputer" "rm -rf $remoteFolder;mkdir $remoteFolder")
command2=("scp" "target/$masterProgram" "$login@$masterComputer:$remoteFolder$masterProgram")
command4=("scp" "$testFileName" "$login@$masterComputer:$remoteFolder$testFileName")
addresses=$(printf ";%s" "${computers[@]}")
addresses=${addresses:1}
args="-path=\"$testFileName\" -s=\"$addresses\" -p=\"$port\""
command5=("ssh" "$login@$masterComputer" "cd $remoteFolder;java -jar $masterProgram $args")

echo ${command1[*]}
"${command1[@]}"
echo ${command2[*]}
"${command2[@]}"
echo ${command3[*]}
"${command3[@]}"
echo ${command4[*]}
"${command4[@]}"
echo ${command5[*]}
"${command5[@]}"
