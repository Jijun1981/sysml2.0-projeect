#!/bin/bash
# 从Eclipse P2仓库获取CDO

echo "Downloading Eclipse Installer to extract CDO..."

# 下载Eclipse IDE包含CDO
wget -q -O /tmp/eclipse-modeling.tar.gz \
  "https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/2023-06/R/eclipse-modeling-2023-06-R-linux-gtk-x86_64.tar.gz&r=1" || {
  echo "Failed to download Eclipse Modeling package"
  
  # 尝试直接获取CDO update site
  echo "Trying CDO update site..."
  
  # 使用Eclipse P2 Director获取CDO
  if [ ! -f /tmp/eclipse-installer ]; then
    wget -q -O /tmp/eclipse-inst.tar.gz \
      "https://www.eclipse.org/downloads/download.php?file=/oomph/products/eclipse-inst-jre-linux64.tar.gz&r=1"
    tar -xzf /tmp/eclipse-inst.tar.gz -C /tmp
  fi
}

# 备选方案：从Eclipse Simrel获取
echo "Alternative: Getting CDO from Eclipse SimRel..."

SIMREL_URL="https://download.eclipse.org/releases/2023-06"
CDO_VERSION="4.23.0"

# CDO核心组件列表
CDO_PLUGINS=(
  "org.eclipse.emf.cdo_${CDO_VERSION}"
  "org.eclipse.emf.cdo.common_${CDO_VERSION}"
  "org.eclipse.emf.cdo.server_4.12.0"
  "org.eclipse.emf.cdo.server.db_4.12.0"
  "org.eclipse.net4j_4.19.0"
  "org.eclipse.net4j.db_4.9.0"
  "org.eclipse.net4j.util_3.23.0"
)

# 从compositeContent获取实际JAR位置
echo "Fetching CDO JARs from SimRel..."
for plugin in "${CDO_PLUGINS[@]}"; do
  # 尝试不同的URL模式
  for pattern in \
    "${SIMREL_URL}/plugins/${plugin}.jar" \
    "${SIMREL_URL}/201906121402/plugins/${plugin}.jar" \
    "https://download.eclipse.org/modeling/emf/cdo/drops/R20230612-0935/plugins/${plugin}.jar"
  do
    echo "Trying: $pattern"
    if wget --spider -q "$pattern" 2>/dev/null; then
      wget -q -O "/mnt/d/sysml2/server/libs/cdo/${plugin}.jar" "$pattern" && \
        echo "Downloaded: ${plugin}.jar" || \
        echo "Failed: ${plugin}.jar"
      break
    fi
  done
done

ls -la /mnt/d/sysml2/server/libs/cdo/
