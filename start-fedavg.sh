#!/bin/bash

./test-network/network.sh up createChannel -c mychannel -ca
./test-network/network.sh deployCCAAS  -ccn fedavg-chaincode -ccp .. -ccep "OR('Org1MSP.peer','Org2MSP.peer')"