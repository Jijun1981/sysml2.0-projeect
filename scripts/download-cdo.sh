#!/bin/bash
# 下载CDO 4.23.0依赖

LIBS_DIR="/mnt/d/sysml2/server/libs/cdo"
mkdir -p $LIBS_DIR

# 使用Maven Central
echo "Downloading CDO dependencies from Maven Central..."

# CDO核心
wget -q -O "$LIBS_DIR/org.eclipse.emf.cdo-4.23.0.jar" \
  "https://repo1.maven.org/maven2/org/eclipse/emf/cdo/org.eclipse.emf.cdo/4.23.0/org.eclipse.emf.cdo-4.23.0.jar" || \
  echo "CDO not found in Maven Central, trying Eclipse..."

# 尝试从Eclipse P2仓库提取
if [ ! -f "$LIBS_DIR/org.eclipse.emf.cdo-4.23.0.jar" ]; then
  echo "Downloading from Eclipse repository..."
  
  # 下载整个CDO SDK
  wget -q -O /tmp/cdo-sdk.zip \
    "https://www.eclipse.org/downloads/download.php?file=/modeling/emf/cdo/drops/R20230612-0935/emf-cdo-SDK-4.23.0.zip&r=1"
  
  if [ -f /tmp/cdo-sdk.zip ]; then
    unzip -q -j /tmp/cdo-sdk.zip "plugins/*.jar" -d $LIBS_DIR
    rm /tmp/cdo-sdk.zip
    echo "CDO JARs extracted successfully"
  fi
fi

ls -la $LIBS_DIR
