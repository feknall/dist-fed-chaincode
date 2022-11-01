#!/bin/bash

./test-network/network.sh createChannel -c mychannel -ca
./test-network/network.sh deployCCAAS  -ccn dist-fed-chaincode -ccp ../ -ccep "OR('Org1MSP.peer','Org2MSP.peer')"

