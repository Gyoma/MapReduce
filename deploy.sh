#!/bin/bash

mvn package

login="agolovin-22"
remoteFolder="/tmp/$login/"
masterProgram="master-program.jar"
serverProgram="server-program.jar"

port="10325"
computers=("tp-1a201-33" "tp-1a201-36" "tp-1a201-37" "tp-1a201-38")
masterComputer="tp-1a201-32"

testFileName="test.txt"

for c in ${computers[@]}; do
  command0=("ssh" "$login@$c" "lsof -ti tcp:$port | xargs kill -9")
  command1=("ssh" "$login@$c" "rm -rf $remoteFolder;mkdir $remoteFolder")
  command2=("scp" "target/$serverProgram" "$login@$c:$remoteFolder$serverProgram")
  command3=("ssh" "$login@$c" "cd $remoteFolder;java -jar $serverProgram")
  echo ${command0[*]}
  "${command0[@]}"
  echo ${command1[*]}
  "${command1[@]}"
  echo ${command2[*]}
  "${command2[@]}"
  echo ${command3[*]}
  "${command3[@]}" &
done

command1=("ssh" "$login@$masterComputer" "rm -rf $remoteFolder;mkdir $remoteFolder")
command2=("scp" "target/$masterProgram" "$login@$masterComputer:$remoteFolder$masterProgram")
command4=("scp" "$testFileName" "$login@$masterComputer:$remoteFolder$testFileName")
addresses=$(printf ";%s" "${computers[@]}")
addresses=${addresses:1}
args="-p=\"$testFileName\" -s=\"$addresses\""
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
