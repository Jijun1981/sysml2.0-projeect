#!/bin/bash
cd /mnt/d/sysml2/opensource/SysML-v2-API-Services

# Kill any existing service
echo "Stopping any existing service..."
pkill -f "sbt.*run" || true
sleep 2

# Set environment variables
export JAVA_OPTS="-Xmx2g"

# Start the service
echo "Starting SysML v2 API Service..."
./sbt/bin/sbt -Dconfig.file=conf/application.conf "~run 9000"