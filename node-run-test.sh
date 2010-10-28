PROTOBUF=/home/notroot/protobuf/protobuf-2.3.0/src
export PROTOBUF
NODE_PATH=/home/notroot/protobuf-for-node/protobuf-for-node/build/default
export NODE_PATH
DYLD_LIBRARY_PATH=/home/notroot/protobuf-for-node/protobuf-for-node/build/default$DYLD_LIBRARY_PATH
export DYLD_LIBRARY_PATH
LD_LIBRARY_PATH=/home/notroot/protobuf-for-node/protobuf-for-node/build/default:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH

cd src/main/nodejs
node actor-test.js