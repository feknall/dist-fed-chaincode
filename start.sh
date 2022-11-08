#!/bin/bash

./test-network/network.sh up createChannel -c mychannel -ca
./test-network/network.sh deployCCAAS  -ccn dist-fed-chaincode -ccp .. -ccep "OR('Org1MSP.peer','Org2MSP.peer')" -cccg '../collections_config.json'

