#!/bin/bash

#
# Each time something changes in Cassandra sources build needs to be executed with: ant build
# CASSANDRA_HOME clashes with CCM, so I have to set it manually.
#
export CASSANDRA_HOME="/Users/mareklewandowski/Magisterka/reactive-transactions/cassandra"
cd $CASSANDRA_HOME
ant build
(cd /Users/mareklewandowski/Magisterka/reactive-transactions/cts && sbt cts-server-ext/toC && cd -)
ccm stop && ccm remove
ccm create -n 3 --install-dir=/Users/mareklewandowski/Magisterka/reactive-transactions/cassandra cts
ccm start
ccm node1 nodetool status

